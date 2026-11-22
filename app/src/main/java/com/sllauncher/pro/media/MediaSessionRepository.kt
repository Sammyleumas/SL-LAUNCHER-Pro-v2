package com.sllauncher.pro.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.sllauncher.pro.notifications.SLNotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaSessions"

/** What is playing in any app that publishes an Android media session (Spotify, YouTube Music, Audiomack, Boomplay, VLC, SL's own player...). */
data class NowPlaying(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val art: Bitmap?,
    val durationMs: Long,
    val positionMs: Long,
    /** elapsedRealtime when [positionMs] was measured. */
    val positionUpdatedAt: Long,
    val speed: Float,
    val isPlaying: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
    val canSeek: Boolean,
) {
    /** Position now, extrapolated from the last update while playing. */
    fun currentPosition(nowElapsed: Long = SystemClock.elapsedRealtime()): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((nowElapsed - positionUpdatedAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/**
 * Follows active media sessions. Android only allows reading other apps'
 * sessions for an app with notification access, so this starts when
 * [SLNotificationListener] connects.
 */
@Singleton
class MediaSessionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, SLNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private var controller: MediaController? = null
    private var started = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = pickController(runCatching { manager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList()))
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        pickController(controllers.orEmpty())
    }

    fun start() {
        if (started) return
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            started = true
            pickController(manager.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification access not granted; media sessions unavailable", e)
        }
    }

    fun stop() {
        if (!started) return
        runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        started = false
        _nowPlaying.value = null
    }

    /** Prefers a session that is playing; otherwise the most recent one. */
    private fun pickController(controllers: List<MediaController>) {
        val chosen = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = chosen
            chosen?.registerCallback(controllerCallback, handler)
        }
        publish()
    }

    private fun publish() {
        val c = controller ?: run { _nowPlaying.value = null; return }
        val meta = c.metadata
        val state = c.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (title.isNullOrBlank()) { _nowPlaying.value = null; return }
        val actions = state?.actions ?: 0L
        _nowPlaying.value = NowPlaying(
            packageName = c.packageName,
            appLabel = runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
            }.getOrDefault(c.packageName),
            title = title,
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = state?.position ?: 0L,
            positionUpdatedAt = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed ?: 1f,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
        )
    }

    /** True when a session exists to control. */
    val hasSession: Boolean get() = controller != null

    fun play(): Boolean = controller?.transportControls?.play()?.let { true } ?: false
    fun pause(): Boolean = controller?.transportControls?.pause()?.let { true } ?: false
    fun togglePlayPause(): Boolean = if (_nowPlaying.value?.isPlaying == true) pause() else play()
    fun next(): Boolean = controller?.transportControls?.skipToNext()?.let { true } ?: false
    fun previous(): Boolean = controller?.transportControls?.skipToPrevious()?.let { true } ?: false
    fun seekTo(ms: Long) { controller?.transportControls?.seekTo(ms) }
    fun openApp() { controller?.sessionActivity?.let { runCatching { it.send() } } }
}
