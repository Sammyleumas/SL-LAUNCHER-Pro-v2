package com.sllauncher.pro.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A song stored on the phone. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    /** Album artwork as stored by the media scanner; may be missing for some files. */
    val artUri: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

data class Album(val id: Long, val title: String, val artist: String, val tracks: List<Track>)
data class Artist(val name: String, val tracks: List<Track>)

/** Reads the device's music from MediaStore (needs the audio permission). */
@Singleton
class MusicLibrary @Inject constructor(@param:ApplicationContext private val context: Context) {

    suspend fun tracks(): List<Track> = withContext(Dispatchers.IO) {
        val out = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    out += Track(
                        id = c.getLong(0),
                        title = c.getString(1) ?: "Unknown title",
                        artist = c.getString(2)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                        album = c.getString(3) ?: "Unknown album",
                        albumId = c.getLong(4),
                        durationMs = c.getLong(5),
                        dateAdded = c.getLong(6) * 1000,
                    )
                }
            }
        }
        out
    }
}

fun albumsOf(tracks: List<Track>): List<Album> =
    tracks.groupBy { it.albumId }.map { (id, list) -> Album(id, list.first().album, list.first().artist, list) }.sortedBy { it.title.lowercase() }

fun artistsOf(tracks: List<Track>): List<Artist> =
    tracks.groupBy { it.artist }.map { (name, list) -> Artist(name, list) }.sortedBy { it.name.lowercase() }
