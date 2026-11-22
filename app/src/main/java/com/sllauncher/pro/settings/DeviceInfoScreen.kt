package com.sllauncher.pro.settings

import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sllauncher.pro.data.apps.AppRepository
import com.sllauncher.pro.device.BatteryRepository
import com.sllauncher.pro.device.BatteryState
import com.sllauncher.pro.device.PowerSource
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLProgressBar
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class DeviceFacts(
    val model: String,
    val android: String,
    val securityPatch: String,
    val storageUsed: Long,
    val storageTotal: Long,
    val ramAvailable: Long,
    val ramTotal: Long,
    val lowMemory: Boolean,
    val network: String,
    val bluetooth: String,
    val display: String,
    val apps: Int,
    val uptime: String,
)

@HiltViewModel
class DeviceInfoViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apps: AppRepository,
    battery: BatteryRepository,
) : ViewModel() {
    val battery: Flow<BatteryState> = battery.state

    /** Reads what Android reliably reports. SL does not "clean" memory or "boost" battery: Android manages both. */
    fun facts(): DeviceFacts {
        val stat = StatFs(Environment.getDataDirectory().path)
        val mem = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val network = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Wi-Fi, connected" else "Wi-Fi, no internet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
        val bt = context.getSystemService(BluetoothManager::class.java)?.adapter
        val bluetooth = when {
            bt == null -> "Not available"
            runCatching { bt.isEnabled }.getOrDefault(false) -> "On"
            else -> "Off"
        }
        val dm = context.resources.displayMetrics
        val refresh = runCatching {
            context.getSystemService(android.hardware.display.DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate
        }.getOrNull()
        val up = SystemClock.elapsedRealtime() / 1000
        return DeviceFacts(
            model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            securityPatch = Build.VERSION.SECURITY_PATCH,
            storageUsed = stat.totalBytes - stat.availableBytes,
            storageTotal = stat.totalBytes,
            ramAvailable = mem.availMem,
            ramTotal = mem.totalMem,
            lowMemory = mem.lowMemory,
            network = network,
            bluetooth = bluetooth,
            display = "${dm.widthPixels} × ${dm.heightPixels} px · ${dm.densityDpi} dpi" + (refresh?.let { " · ${it.toInt()} Hz" } ?: ""),
            apps = apps.apps.value.size,
            uptime = "${up / 3600} h ${(up % 3600) / 60} min",
        )
    }
}

@Composable
fun DeviceInfoRoute(onBack: () -> Unit, viewModel: DeviceInfoViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val battery by viewModel.battery.collectAsStateWithLifecycle(initialValue = null)
    val facts by produceState<DeviceFacts?>(null) { value = viewModel.facts() }
    ScreenScaffold(title = "Device info", onBack = onBack, scrollable = true) {
        val f = facts ?: return@ScreenScaffold
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SLBlock(Modifier.fillMaxWidth(), shadow = true) {
                Text(f.model, style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
                Text(f.android, style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
                Text("Security patch ${f.securityPatch}", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            }
            battery?.let { b ->
                SLSectionHeader("Battery")
                Fact("Level", "${b.percent}%")
                Fact("State", when { b.full -> "Full"; b.charging && b.source == PowerSource.Wireless -> "Charging wirelessly"; b.charging -> "Charging"; else -> "On battery" })
                SLProgressBar(b.percent / 100f, contentDescription = "Battery level")
            }
            SLSectionHeader("Storage")
            Fact("Used", "${Formatter.formatShortFileSize(context, f.storageUsed)} of ${Formatter.formatShortFileSize(context, f.storageTotal)}")
            SLProgressBar(f.storageUsed.toFloat() / f.storageTotal.coerceAtLeast(1), contentDescription = "Storage used")
            SLSectionHeader("Memory")
            Fact("Available", "${Formatter.formatShortFileSize(context, f.ramAvailable)} of ${Formatter.formatShortFileSize(context, f.ramTotal)}")
            if (f.lowMemory) Text("Android reports low memory. It will close background apps by itself.", style = SLTheme.typography.caption, color = SLTheme.colors.error)
            SLSectionHeader("Connections")
            Fact("Network", f.network)
            Fact("Bluetooth", f.bluetooth)
            SLSectionHeader("Display")
            Fact("Screen", f.display)
            SLSectionHeader("Launcher")
            Fact("Installed apps", "${f.apps}")
            Fact("Uptime", f.uptime)
            Text("SL shows what Android reports. It does not \"clean\" memory or \"boost\" battery: Android manages both, and apps that claim to usually make things worse.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = SLTheme.typography.body, color = SLTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = SLTheme.typography.body, color = SLTheme.colors.textPrimary)
    }
}
