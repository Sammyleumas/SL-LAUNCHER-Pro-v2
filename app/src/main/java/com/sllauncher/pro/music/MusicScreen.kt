package com.sllauncher.pro.music

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.sllauncher.pro.common.SystemIntents
import com.sllauncher.pro.common.startSafely
import com.sllauncher.pro.data.db.MusicDao
import com.sllauncher.pro.data.db.PlaylistEntity
import com.sllauncher.pro.data.db.PlaylistTrackEntity
import com.sllauncher.pro.data.db.TrackStatsEntity
import com.sllauncher.pro.media.MediaSessionRepository
import com.sllauncher.pro.media.NowPlaying
import com.sllauncher.pro.ui.ScreenScaffold
import com.sllauncher.core.designsystem.component.SLBlock
import com.sllauncher.core.designsystem.component.SLBottomSheet
import com.sllauncher.core.designsystem.component.SLButton
import com.sllauncher.core.designsystem.component.SLButtonStyle
import com.sllauncher.core.designsystem.component.SLChip
import com.sllauncher.core.designsystem.component.SLEmptyState
import com.sllauncher.core.designsystem.component.SLLoadingIndicator
import com.sllauncher.core.designsystem.component.SLMediaControls
import com.sllauncher.core.designsystem.component.SLNavigationRow
import com.sllauncher.core.designsystem.component.SLSearchField
import com.sllauncher.core.designsystem.component.SLSectionHeader
import com.sllauncher.core.designsystem.component.SLSlider
import com.sllauncher.core.designsystem.theme.SLTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MusicTab(val label: String) { NowPlaying("Now playing"), Songs("Songs"), Albums("Albums"), Artists("Artists"), Playlists("Playlists"), Favorites("Favourites"), Recent("Recent") }

data class MusicUiState(
    val permission: Boolean = false,
    val loading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val stats: Map<Long, TrackStatsEntity> = emptyMap(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val player: PlayerState = PlayerState(),
    val external: NowPlaying? = null,
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val library: MusicLibrary,
    val controller: MusicController,
    private val dao: MusicDao,
    private val sessions: MediaSessionRepository,
) : ViewModel() {
    private val tracks = MutableStateFlow<List<Track>?>(null)
    private val permission = MutableStateFlow(hasPermission())

    val uiState: StateFlow<MusicUiState> = combine(
        combine(permission, tracks) { p, t -> p to t },
        dao.observeStats(),
        dao.observePlaylists(),
        controller.state,
        sessions.nowPlaying,
    ) { (p, t), stats, playlists, player, external ->
        MusicUiState(
            permission = p,
            loading = p && t == null,
            tracks = t.orEmpty(),
            stats = stats.associateBy { it.mediaId },
            playlists = playlists,
            player = player,
            // Another app's session, shown when SL's own player is idle.
            external = external?.takeIf { it.packageName != context.packageName },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MusicUiState())

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(context, audioPermission()) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun onPermissionResult() {
        permission.value = hasPermission()
        load()
    }

    fun load() {
        if (!permission.value) return
        viewModelScope.launch { tracks.value = library.tracks() }
    }

    fun toggleFavorite(track: Track) = viewModelScope.launch {
        val s = dao.stats(track.id) ?: TrackStatsEntity(track.id)
        dao.upsertStats(s.copy(favorite = !s.favorite))
    }

    fun createPlaylist(name: String, first: Track?) = viewModelScope.launch {
        val id = dao.insertPlaylist(PlaylistEntity(name = name.trim().take(40), createdAt = System.currentTimeMillis()))
        if (first != null) dao.insertTrack(PlaylistTrackEntity(id, 0, first.id))
    }

    fun addToPlaylist(playlist: PlaylistEntity, track: Track) = viewModelScope.launch {
        dao.insertTrack(PlaylistTrackEntity(playlist.id, dao.lastPosition(playlist.id) + 1, track.id))
    }

    fun deletePlaylist(playlist: PlaylistEntity) = viewModelScope.launch { dao.deletePlaylist(playlist) }

    fun playlistTracks(playlistId: Long) = dao.observePlaylistTracks(playlistId)

    fun external() = sessions

    companion object {
        fun audioPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/** Music center: local library, SL's player, and controls for other apps' playback. */
@Composable
fun MusicRoute(onBack: () -> Unit, viewModel: MusicViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(MusicTab.NowPlaying) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<Track?>(null) }
    var newPlaylistFor by remember { mutableStateOf<Track?>(null) }
    var showNewPlaylist by remember { mutableStateOf(false) }
    var openPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.onPermissionResult() }

    LifecycleResumeEffect(Unit) {
        viewModel.onPermissionResult()
        onPauseOrDispose { }
    }

    ScreenScaffold(title = "Music", onBack = onBack) {
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(MusicTab.entries) { t -> SLChip(t.label, tab == t, { tab = t }) }
        }
        if (tab == MusicTab.NowPlaying) {
            NowPlayingPane(state, viewModel, context)
            return@ScreenScaffold
        }
        if (!state.permission) {
            SLBlock(Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Play music stored on this phone", style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
                Text("SL needs access to audio files to list and play your songs. Nothing is uploaded.", style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary)
                SLButton("Allow access to music", { permissionLauncher.launch(MusicViewModel.audioPermission()) })
            }
            return@ScreenScaffold
        }
        if (state.loading) {
            SLLoadingIndicator(Modifier.padding(24.dp))
            return@ScreenScaffold
        }
        if (tab == MusicTab.Songs || tab == MusicTab.Favorites || tab == MusicTab.Recent) {
            SLSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search songs and artists", modifier = Modifier.padding(horizontal = 12.dp))
        }
        val filtered = state.tracks.filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) }
        when (tab) {
            MusicTab.Songs -> TrackList(filtered, state, viewModel, onMenu = { menuFor = it }, empty = "No songs found on this phone.")
            MusicTab.Favorites -> TrackList(filtered.filter { state.stats[it.id]?.favorite == true }, state, viewModel, onMenu = { menuFor = it }, empty = "Long-press a song and choose Favourite.")
            MusicTab.Recent -> TrackList(
                filtered.filter { (state.stats[it.id]?.lastPlayedAt ?: 0) > 0 }.sortedByDescending { state.stats[it.id]?.lastPlayedAt ?: 0 }.take(50),
                state, viewModel, onMenu = { menuFor = it }, empty = "Songs you play will appear here.",
            )
            MusicTab.Albums -> LazyColumn(contentPadding = PaddingValues(12.dp)) {
                items(albumsOf(state.tracks), key = { it.id }) { album ->
                    SLNavigationRow(title = album.title, subtitle = "${album.artist} · ${album.tracks.size} songs", onClick = { viewModel.controller.play(album.tracks, 0); tab = MusicTab.NowPlaying })
                }
            }
            MusicTab.Artists -> LazyColumn(contentPadding = PaddingValues(12.dp)) {
                items(artistsOf(state.tracks), key = { it.name }) { artist ->
                    SLNavigationRow(title = artist.name, subtitle = "${artist.tracks.size} songs", onClick = { viewModel.controller.play(artist.tracks, 0); tab = MusicTab.NowPlaying })
                }
            }
            MusicTab.Playlists -> {
                SLButton("New playlist", { newPlaylistFor = null; showNewPlaylist = true }, style = SLButtonStyle.Secondary, modifier = Modifier.padding(12.dp))
                if (state.playlists.isEmpty()) SLEmptyState("No playlists yet", "Create one, then long-press songs to add them.")
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    items(state.playlists, key = { it.id }) { p -> SLNavigationRow(title = p.name, onClick = { openPlaylist = p }) }
                }
            }
            MusicTab.NowPlaying -> Unit
        }
    }

    menuFor?.let { track ->
        val fav = state.stats[track.id]?.favorite == true
        SLBottomSheet(onDismissRequest = { menuFor = null }, title = track.title) {
            SLNavigationRow(title = if (fav) "Remove from favourites" else "Favourite", onClick = { viewModel.toggleFavorite(track); menuFor = null })
            state.playlists.forEach { p -> SLNavigationRow(title = "Add to ${p.name}", onClick = { viewModel.addToPlaylist(p, track); menuFor = null }) }
            SLNavigationRow(title = "New playlist with this song", onClick = { newPlaylistFor = track; showNewPlaylist = true; menuFor = null })
        }
    }
    if (showNewPlaylist) {
        var name by remember { mutableStateOf("") }
        SLBottomSheet(onDismissRequest = { showNewPlaylist = false }, title = "New playlist") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SLSearchField(query = name, onQueryChange = { name = it }, placeholder = "Playlist name")
                SLButton("Create", { viewModel.createPlaylist(name, newPlaylistFor); showNewPlaylist = false }, enabled = name.isNotBlank())
            }
        }
    }
    openPlaylist?.let { playlist ->
        val rows by viewModel.playlistTracks(playlist.id).collectAsStateWithLifecycle(emptyList())
        val byId = state.tracks.associateBy { it.id }
        val list = rows.mapNotNull { byId[it.mediaId] }
        SLBottomSheet(onDismissRequest = { openPlaylist = null }, title = playlist.name) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLButton("Play", { viewModel.controller.play(list, 0); openPlaylist = null; tab = MusicTab.NowPlaying }, enabled = list.isNotEmpty())
                SLButton("Shuffle", { viewModel.controller.play(list, 0, shuffle = true); openPlaylist = null; tab = MusicTab.NowPlaying }, enabled = list.isNotEmpty(), style = SLButtonStyle.Secondary)
                SLButton("Delete", { viewModel.deletePlaylist(playlist); openPlaylist = null }, style = SLButtonStyle.Ghost)
            }
            list.forEachIndexed { i, t -> SLNavigationRow(title = t.title, subtitle = t.artist, onClick = { viewModel.controller.play(list, i); openPlaylist = null; tab = MusicTab.NowPlaying }) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackList(tracks: List<Track>, state: MusicUiState, viewModel: MusicViewModel, onMenu: (Track) -> Unit, empty: String) {
    if (tracks.isEmpty()) {
        SLEmptyState("Nothing here", empty)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                SLButton("Play all", { viewModel.controller.play(tracks, 0) })
                SLButton("Shuffle", { viewModel.controller.play(tracks, (tracks.indices).random(), shuffle = true) }, style = SLButtonStyle.Secondary)
            }
        }
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, t ->
            val current = state.player.mediaId == t.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .combinedClickable(onClick = { viewModel.controller.play(tracks, index) }, onLongClick = { onMenu(t) }, onLongClickLabel = "Song options", role = Role.Button)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(model = t.artUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).background(SLTheme.colors.surfaceElevated))
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = SLTheme.typography.body, color = if (current) SLTheme.colors.accentIndicator else SLTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary, maxLines = 1)
                }
                if (state.stats[t.id]?.favorite == true) Text("★", color = SLTheme.colors.accentIndicator)
                Text(formatMs(t.durationMs), style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun NowPlayingPane(state: MusicUiState, viewModel: MusicViewModel, context: Context) {
    val player = state.player
    if (!player.hasMedia) {
        val ext = state.external
        if (ext != null) {
            ExternalPlayerCard(ext, viewModel)
        } else {
            SLEmptyState(
                title = "Nothing playing",
                body = "Pick a song from Songs, Albums or Artists. Music playing in other apps (Spotify, Audiomack, Boomplay…) appears here too once notification access is on.",
            )
        }
        return
    }
    var position by remember { mutableFloatStateOf(0f) }
    var seeking by remember { mutableStateOf(false) }
    LaunchedEffect(player.isPlaying, player.mediaId) {
        while (true) {
            if (!seeking) position = viewModel.controller.positionMs().toFloat()
            if (!player.isPlaying) break
            delay(500)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AsyncImage(
                model = player.artUri,
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f).background(SLTheme.colors.surfaceElevated),
            )
        }
        item {
            Text(player.title ?: "Unknown", style = SLTheme.typography.headline, color = SLTheme.colors.textPrimary)
            Text(player.artist ?: "", style = SLTheme.typography.body, color = SLTheme.colors.textSecondary)
        }
        item {
            SLSlider(
                value = position.coerceIn(0f, player.durationMs.coerceAtLeast(1).toFloat()),
                onValueChange = { seeking = true; position = it },
                onValueChangeFinished = { viewModel.controller.seekTo(position.toLong()); seeking = false },
                contentDescription = "Seek",
                valueRange = 0f..player.durationMs.coerceAtLeast(1).toFloat(),
            )
            Row {
                Text(formatMs(position.toLong()), style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                Text(formatMs(player.durationMs), style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary)
            }
        }
        item {
            SLMediaControls(isPlaying = player.isPlaying, onPlayPause = { viewModel.controller.playPause() }, onPrevious = { viewModel.controller.previous() }, onNext = { viewModel.controller.next() })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SLChip(if (player.shuffle) "Shuffle on" else "Shuffle off", player.shuffle, { viewModel.controller.toggleShuffle() })
                SLChip("Repeat ${player.repeat.name.lowercase()}", player.repeat != RepeatMode.Off, { viewModel.controller.cycleRepeat() })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLButton("Audio output", { context.startSafely(SystemIntents.bluetooth()) }, style = SLButtonStyle.Secondary)
                SLButton("Equalizer", {
                    val ok = context.startSafely(
                        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                            .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioSessionHolder.audioSessionId)
                            .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC),
                    )
                    if (!ok) context.startSafely(SystemIntents.sound())
                }, style = SLButtonStyle.Secondary)
            }
        }
        if (player.queue.size > 1) {
            item { SLSectionHeader("Up next") }
            itemsIndexed(player.queue) { i, (_, title) ->
                SLNavigationRow(title = title, subtitle = if (i == player.queueIndex) "Playing" else null, onClick = { viewModel.controller.skipToQueueItem(i) })
            }
        }
        item { Text("Lyrics need a licensed lyrics provider, which isn't set up.", style = SLTheme.typography.caption, color = SLTheme.colors.textSecondary) }
    }
}

@Composable
private fun ExternalPlayerCard(np: NowPlaying, viewModel: MusicViewModel) {
    SLBlock(Modifier.padding(16.dp).fillMaxWidth(), shadow = true) {
        Text("PLAYING IN ${np.appLabel.uppercase()}", style = SLTheme.typography.label, color = SLTheme.colors.accentIndicator)
        Text(np.title, style = SLTheme.typography.title, color = SLTheme.colors.textPrimary)
        np.artist?.let { Text(it, style = SLTheme.typography.bodySmall, color = SLTheme.colors.textSecondary) }
        SLMediaControls(
            isPlaying = np.isPlaying,
            onPlayPause = { viewModel.external().togglePlayPause() },
            onPrevious = { viewModel.external().previous() },
            onNext = { viewModel.external().next() },
            canSkipNext = np.canSkipNext,
            canSkipPrevious = np.canSkipPrevious,
        )
        SLButton("Open ${np.appLabel}", { viewModel.external().openApp() }, style = SLButtonStyle.Ghost)
    }
}

fun formatMs(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
