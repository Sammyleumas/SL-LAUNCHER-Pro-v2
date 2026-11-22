package com.sllauncher.pro.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * Intents into Android's own screens and panels. SL never changes protected
 * system settings directly; it opens the screen where the user can.
 */
object SystemIntents {
    fun wifi(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_WIFI)
    } else {
        Intent(Settings.ACTION_WIFI_SETTINGS)
    }

    fun internet(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
        Intent(Settings.ACTION_WIRELESS_SETTINGS)
    }

    fun volumePanel(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_VOLUME)
    } else {
        Intent(Settings.ACTION_SOUND_SETTINGS)
    }

    fun bluetooth() = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun airplane() = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    fun display() = Intent(Settings.ACTION_DISPLAY_SETTINGS)
    fun battery() = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
    fun hotspot() = Intent(Settings.ACTION_WIRELESS_SETTINGS)
    fun sound() = Intent(Settings.ACTION_SOUND_SETTINGS)
    fun deviceSettings() = Intent(Settings.ACTION_SETTINGS)
    fun homeSettings() = Intent(Settings.ACTION_HOME_SETTINGS)
    fun notificationListenerSettings() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    fun writeSettings(context: Context) = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
    fun appDetails(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun appNotificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    /** Android's wallpaper chooser (gallery, live wallpapers, system styles). */
    fun wallpaperPicker(): Intent = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null)

    fun camera() = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

    /** Hands a timer to the clock app, which runs and rings it reliably. */
    fun setTimer(seconds: Int, label: String?): Intent = Intent(AlarmClock.ACTION_SET_TIMER)
        .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
        .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        .apply { if (label != null) putExtra(AlarmClock.EXTRA_MESSAGE, label) }

    fun setAlarm(hour: Int, minute: Int, label: String?): Intent = Intent(AlarmClock.ACTION_SET_ALARM)
        .putExtra(AlarmClock.EXTRA_HOUR, hour)
        .putExtra(AlarmClock.EXTRA_MINUTES, minute)
        .apply { if (label != null) putExtra(AlarmClock.EXTRA_MESSAGE, label) }

    fun webSearch(query: String) = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query)

    fun dial(number: String? = null) = Intent(Intent.ACTION_DIAL, number?.let { Uri.parse("tel:$it") })

    fun openUrl(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    fun share(text: String, subject: String? = null): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            .apply { if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject) },
        null,
    )
}

/** Starts [intent], returning false instead of crashing when no app can handle it. */
fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    Log.w("SystemIntents", "No activity for ${intent.action}", e)
    false
} catch (e: SecurityException) {
    Log.w("SystemIntents", "Not allowed: ${intent.action}", e)
    false
}
