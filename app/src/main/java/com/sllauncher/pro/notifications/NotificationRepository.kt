package com.sllauncher.pro.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sllauncher.pro.data.db.NotificationHistoryDao
import com.sllauncher.pro.data.db.NotificationHistoryEntity
import com.sllauncher.pro.data.settings.SettingsRepository
import com.sllauncher.pro.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Notifications"

/** A notification action. [remoteInput] is set when the app supports inline replies. */
data class SLNotificationAction(
    val title: String,
    val intent: PendingIntent,
    val remoteInput: RemoteInput?,
    val semantic: Int,
)

/** One message inside a messaging-style notification. */
data class SLMessage(val sender: String?, val text: String, val time: Long)

/** The launcher's view of an active notification. Contents never leave the phone. */
data class SLNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val category: String?,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val groupKey: String,
    val isGroupSummary: Boolean,
    val contentIntent: PendingIntent?,
    val actions: List<SLNotificationAction>,
    val progressMax: Int,
    val progress: Int,
    val progressIndeterminate: Boolean,
    val messages: List<SLMessage>,
    val conversationTitle: String?,
    val count: Int,
    val chronometerBase: Long?,
) {
    val replyAction: SLNotificationAction? get() = actions.firstOrNull { it.remoteInput != null && it.remoteInput.allowFreeFormInput }
    val isMessage: Boolean get() = category == Notification.CATEGORY_MESSAGE || messages.isNotEmpty()
    val isCall: Boolean get() = category == Notification.CATEGORY_CALL
    val isProgress: Boolean get() = progressMax > 0 || progressIndeterminate
}

/** Result of sending a quick reply. */
enum class ReplyResult { Sent, NotSupported, Failed }

/**
 * Active notifications delivered by [SLNotificationListener]. Empty until
 * the user grants notification access in Android settings.
 */
@Singleton
class NotificationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val historyDao: NotificationHistoryDao,
    private val settings: SettingsRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _notifications = MutableStateFlow<List<SLNotification>>(emptyList())
    val notifications: StateFlow<List<SLNotification>> = _notifications.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** Set while the listener service is bound; used for dismissing. */
    @Volatile internal var service: SLNotificationListener? = null

    fun hasAccess(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(context, SLNotificationListener::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    internal fun onConnected(service: SLNotificationListener, active: Array<StatusBarNotification>) {
        this.service = service
        _connected.value = true
        _notifications.value = active.mapNotNull(::toModel).sortedByDescending { it.postTime }
    }

    internal fun onDisconnected() {
        service = null
        _connected.value = false
        _notifications.value = emptyList()
    }

    internal fun onPosted(sbn: StatusBarNotification) {
        val model = toModel(sbn) ?: return
        _notifications.value = (listOf(model) + _notifications.value.filter { it.key != model.key }).sortedByDescending { it.postTime }
        recordHistory(model)
    }

    internal fun onRemoved(sbn: StatusBarNotification) {
        _notifications.value = _notifications.value.filter { it.key != sbn.key }
    }

    /** Unread count per package, for app icon badges. Group summaries are not double counted. */
    fun badgeCount(packageName: String): Int =
        _notifications.value.count { it.packageName == packageName && !it.isGroupSummary && it.isClearable }

    fun dismiss(key: String) {
        runCatching { service?.cancelNotification(key) }.onFailure { Log.w(TAG, "dismiss failed", it) }
    }

    fun dismissAll() {
        runCatching { service?.cancelAllNotifications() }.onFailure { Log.w(TAG, "dismissAll failed", it) }
    }

    fun open(notification: SLNotification): Boolean = send(notification.contentIntent, null)

    fun runAction(action: SLNotificationAction): Boolean = send(action.intent, null)

    /**
     * Sends [text] through the notification's own reply action, exactly as
     * the notification shade does. Apps that do not offer inline replies
     * cannot be replied to from SL.
     */
    fun reply(notification: SLNotification, text: String): ReplyResult {
        val action = notification.replyAction ?: return ReplyResult.NotSupported
        val input = action.remoteInput ?: return ReplyResult.NotSupported
        val results = Bundle().apply { putCharSequence(input.resultKey, text) }
        val fillIn = Intent()
        RemoteInput.addResultsToIntent(arrayOf(input), fillIn, results)
        return if (send(action.intent, fillIn)) ReplyResult.Sent else ReplyResult.Failed
    }

    /** Newest message notification the user can reply to. */
    fun latestReplyable(): SLNotification? = _notifications.value.firstOrNull { it.isMessage && it.replyAction != null }

    private fun send(intent: PendingIntent?, fillIn: Intent?): Boolean {
        if (intent == null) return false
        return try {
            if (fillIn != null) intent.send(context, 0, fillIn) else intent.send()
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "PendingIntent cancelled", e)
            false
        }
    }

    private fun recordHistory(model: SLNotification) {
        val s = settings.current
        if (!s.notificationHistoryEnabled || model.isOngoing || model.isGroupSummary || model.packageName == context.packageName) return
        scope.launch {
            historyDao.insert(NotificationHistoryEntity(packageName = model.packageName, appLabel = model.appLabel, title = model.title, text = model.text, postedAt = model.postTime))
            historyDao.pruneBefore(System.currentTimeMillis() - 7L * 24 * 3600 * 1000)
        }
    }

    private fun toModel(sbn: StatusBarNotification): SLNotification? {
        val n = sbn.notification ?: return null
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        // Skip invisible system placeholders (e.g. "app running in background" with no text).
        if (title.isEmpty() && text.isEmpty() && n.category != Notification.CATEGORY_TRANSPORT) return null

        val style = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }.getOrNull()
        val messages = style?.messages?.takeLast(5)?.map {
            SLMessage(it.person?.name?.toString(), it.text?.toString().orEmpty(), it.timestamp)
        }.orEmpty()

        val actions = n.actions.orEmpty().mapNotNull { a ->
            val pi = a.actionIntent ?: return@mapNotNull null
            SLNotificationAction(
                title = a.title?.toString().orEmpty(),
                intent = pi,
                remoteInput = a.remoteInputs?.firstOrNull { it.allowFreeFormInput },
                semantic = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) a.semanticAction else 0,
            )
        }

        val label = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)

        return SLNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = label,
            title = title,
            text = text,
            postTime = sbn.postTime,
            category = n.category,
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable,
            groupKey = sbn.groupKey ?: sbn.key,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            contentIntent = n.contentIntent,
            actions = actions,
            progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
            progress = extras.getInt(Notification.EXTRA_PROGRESS, 0),
            progressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false),
            messages = messages,
            conversationTitle = style?.conversationTitle?.toString(),
            count = n.number,
            chronometerBase = if (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)) n.`when` else null,
        )
    }
}
