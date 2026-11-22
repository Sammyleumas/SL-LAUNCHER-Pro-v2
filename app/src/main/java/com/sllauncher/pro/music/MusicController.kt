package com.sllauncher.pro.music

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sllauncher.pro.data.db.MusicDao
import com.sllauncher.pro.data.db.TrackStatsEntity
import com.sllauncher.pro.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class RepeatMode { Off, All, One }

data class PlayerState(
    val connected: Boolean = false,
    val mediaId: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val artUri: String? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queue: List<Pair<Long, String>> = emptyList(),
    val queueIndex: Int = -1,
) {
    val hasMedia: Boolean get() = mediaId != null
}

/** Controls SL's own player through its media session. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Singleton
class MusicController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull() ?: return
            scope.launch {
                val stats = musicDao.stats(id) ?: TrackStatsEntity(id)
                musicDao.upsertStats(stats.copy(playCount = stats.playCount + 1, lastPlayedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun connect(): MediaController {
        controller?.let { return it }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val c = suspendCancellableCoroutine { cont ->
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
            cont.invokeOnCancellation { future.cancel(true) }
        }
        c.addListener(listener)
        controller = c
        publish(c)
        return c
    }

    /** Plays [tracks] starting at [index], replacing the queue. */
    fun play(tracks: List<Track>, index: Int, shuffle: Boolean = false) = scope.launch(kotlinx.coroutines.Dispatchers.Main) {
        val c = connect()
        c.setMediaItems(tracks.map(::toMediaItem), index.coerceIn(0, (tracks.size - 1).coerceAtLeast(0)), 0L)
        c.shuffleModeEnabled = shuffle
        c.prepare()
        c.play()
    }

    fun playPause() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun pause() = withController { it.pause() }
    fun resume() = withController { if (it.mediaItemCount > 0) it.play() }
    fun next() = withController { it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun skipToQueueItem(index: Int) = withController { it.seekToDefaultPosition(index) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Current position; read on demand (the UI polls while visible). */
    fun positionMs(): Long = controller?.currentPosition ?: 0L

    private fun withController(block: (MediaController) -> Unit) = scope.launch(kotlinx.coroutines.Dispatchers.Main) { block(connect()) }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlayerState(
            connected = true,
            mediaId = item?.mediaId?.toLongOrNull(),
            title = item?.mediaMetadata?.title?.toString(),
            artist = item?.mediaMetadata?.artist?.toString(),
            artUri = item?.mediaMetadata?.artworkUri?.toString(),
            isPlaying = player.isPlaying,
            durationMs = player.duration.coerceAtLeast(0),
            shuffle = player.shuffleModeEnabled,
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                else -> RepeatMode.Off
            },
            queue = (0 until player.mediaItemCount).map { i ->
                val m = player.getMediaItemAt(i)
                (m.mediaId.toLongOrNull() ?: -1L) to (m.mediaMetadata.title?.toString() ?: "Unknown")
            },
            queueIndex = player.currentMediaItemIndex,
        )
    }

    private fun toMediaItem(track: Track): MediaItem = MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.artUri)
                .build(),
        )
        .build()
}
