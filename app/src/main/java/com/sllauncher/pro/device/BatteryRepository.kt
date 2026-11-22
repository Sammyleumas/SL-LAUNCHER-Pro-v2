package com.sllauncher.pro.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class PowerSource { None, Wired, Wireless }

/**
 * Battery state as Android reports it. SL shows only what the platform
 * reliably exposes: level, charging/full, and wired vs wireless. It makes
 * no fast-charging claims, because there is no reliable cross-device API.
 */
data class BatteryState(val percent: Int, val charging: Boolean, val full: Boolean, val source: PowerSource) {
    val low: Boolean get() = !charging && percent <= 15
}

fun batteryStateFrom(intent: Intent): BatteryState {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val source = when (plugged) {
        0 -> PowerSource.None
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> PowerSource.Wireless
        else -> PowerSource.Wired
    }
    return BatteryState(
        percent = if (level < 0) 0 else level * 100 / scale,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || (source != PowerSource.None && status != BatteryManager.BATTERY_STATUS_DISCHARGING),
        full = status == BatteryManager.BATTERY_STATUS_FULL,
        source = source,
    )
}

@Singleton
class BatteryRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    /** Battery state while collected. ACTION_BATTERY_CHANGED is sticky, so the first value arrives immediately. */
    val state: Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(batteryStateFrom(intent))
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { trySend(batteryStateFrom(it)) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
