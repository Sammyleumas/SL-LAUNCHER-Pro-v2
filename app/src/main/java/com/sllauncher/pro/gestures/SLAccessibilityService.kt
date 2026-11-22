package com.sllauncher.pro.gestures

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Lets the home-screen gestures lock the screen and open the notification
 * shade, which Android only allows through an accessibility service.
 *
 * It listens to no events, reads no screen content and does nothing until
 * the user makes a gesture on SL's home screen. The user turns it on in
 * Settings › Accessibility › SL Launcher Pro, and can turn it off there.
 */
class SLAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        // Ask for no events at all: this service only performs actions.
        serviceInfo = serviceInfo.apply {
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }
        current = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        current = null
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile private var current: WeakReference<SLAccessibilityService>? = null

        val isRunning: Boolean get() = current?.get() != null

        /** Locks the screen (Android 9+). False when the service is off or the phone is too old. */
        fun lockScreen(): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return current?.get()?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
        }

        /** Pulls down the notification shade. False when the service is off. */
        fun openNotificationShade(): Boolean =
            current?.get()?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true

        fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun openSettings(context: Context) {
            runCatching { context.startActivity(settingsIntent()) }
        }
    }
}
