package com.sllauncher.pro.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.util.SizeF
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.sllauncher.pro.data.apps.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WidgetHost"
private const val HOST_ID = 0x51A7

/** An installable widget shown in the widget picker. */
data class WidgetOption(val info: AppWidgetProviderInfo, val label: String, val appLabel: String, val user: UserHandle)

/**
 * Hosts other apps' widgets on the home screen with Android's AppWidgetHost
 * API. Binding a widget needs the user's one-time consent (Android shows a
 * system dialog); SL never binds without it.
 */
@Singleton
class WidgetHostManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host: AppWidgetHost = SLAppWidgetHost(context)

    /** Set by the home screen so widget long-presses open the item menu. */
    var onWidgetLongPress: ((widgetId: Int) -> Unit)? = null

    fun startListening() = runCatching { host.startListening() }.onFailure { Log.w(TAG, "startListening failed", it) }

    fun stopListening() = runCatching { host.stopListening() }.onFailure { Log.w(TAG, "stopListening failed", it) }

    fun availableWidgets(): List<WidgetOption> {
        val pm = context.packageManager
        val users = context.getSystemService(UserManager::class.java).userProfiles
        return users.flatMap { user ->
            appWidgetManager.getInstalledProvidersForProfile(user).map { info ->
                val appLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrDefault(info.provider.packageName)
                WidgetOption(info, info.loadLabel(pm) ?: appLabel, appLabel, user)
            }
        }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
    }

    fun previewOf(option: WidgetOption): Drawable? =
        runCatching { option.info.loadPreviewImage(context, context.resources.displayMetrics.densityDpi) }.getOrNull()
            ?: runCatching { option.info.loadIcon(context, context.resources.displayMetrics.densityDpi) }.getOrNull()

    fun allocateId(): Int = host.allocateAppWidgetId()

    /** Binds without a dialog when the user already allowed SL; otherwise returns false and the caller asks. */
    fun bindIfAllowed(widgetId: Int, option: WidgetOption): Boolean =
        runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, option.user, option.info.provider, null) }.getOrDefault(false)

    fun deleteId(widgetId: Int) = runCatching { host.deleteAppWidgetId(widgetId) }

    fun info(widgetId: Int): AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(widgetId)

    fun createView(widgetId: Int): AppWidgetHostView? {
        val info = info(widgetId) ?: return null
        return host.createView(context, widgetId, info)
    }

    /** Tells the widget how big it is so it can choose a layout (Android 12+ uses exact sizes). */
    fun updateSize(view: AppWidgetHostView, widthDp: Float, heightDp: Float) {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
            }
        }
    }

    /** User-facing label of the app that owns a widget. */
    fun appLabelFor(widgetId: Int): String? = info(widgetId)?.provider?.packageName?.let { pkg ->
        appRepository.apps.value.firstOrNull { it.key.packageName == pkg }?.label ?: pkg
    }

    private inner class SLAppWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
            LongPressWidgetView(context) { onWidgetLongPress?.invoke(appWidgetId) }
    }
}

/**
 * Widget views consume touches, so a launcher has to detect long-press
 * itself to let users move or remove widgets.
 */
@SuppressLint("ViewConstructor")
private class LongPressWidgetView(context: Context, private val onLongPress: () -> Unit) : AppWidgetHostView(context) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false
    private val trigger = Runnable {
        longPressed = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        onLongPress()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressed = false
                downX = ev.x; downY = ev.y
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (kotlin.math.abs(ev.x - downX) > slop || kotlin.math.abs(ev.y - downY) > slop) removeCallbacks(trigger)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        // After a long-press, take the rest of the gesture so the widget does not also get a click.
        return longPressed
    }
}
