package com.sllauncher.pro.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sllauncher.pro.media.MediaSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Notification access for the Dynamic Island, notification center, badges
 * and quick replies. Android only binds this service after the user turns
 * on "Notification access" for SL Launcher in Settings. Notification
 * contents are processed on the phone and never uploaded.
 */
@AndroidEntryPoint
class SLNotificationListener : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository
    @Inject lateinit var media: MediaSessionRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        val active = runCatching { activeNotifications }.getOrNull() ?: emptyArray()
        repository.onConnected(this, active)
        media.start()
    }

    override fun onListenerDisconnected() {
        repository.onDisconnected()
        media.stop()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching { repository.onPosted(sbn) }.onFailure { Log.w("SLNotificationListener", "post failed", it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        repository.onRemoved(sbn)
    }
}
