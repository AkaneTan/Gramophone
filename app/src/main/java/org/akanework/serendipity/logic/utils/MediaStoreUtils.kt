package org.akanework.serendipity.logic.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlin.system.measureTimeMillis

object MediaStoreUtils {

    data class Album(
        val id: Long,
        val title: String,
        val artist: String,
        val albumYear: Int,
        val songList: List<MediaItem>
    )

    data class Artist(
        val id: Long,
        val title: String,
        val songList: List<MediaItem>
    )

    /**
     * [getAllSongs] gets all of your songs from your local disk.
     *
     * @param context
     * @return
     */
    fun getAllSongs(context: Context): Triple<MutableList<MediaItem>, MutableList<Album>, MutableList<Artist>> {
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.NUM_TRACKS,
        )
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"

        val songs = mutableListOf<MediaItem>()
        val albumMap = mutableMapOf<Pair<String, Int>, MutableList<MediaItem>>()
        val artistMap = mutableMapOf<String, MutableList<MediaItem>>()
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val discNumberColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER)
            val trackNumberColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.NUM_TRACKS)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val albumArtist = it.getString(albumArtistColumn)
                    ?: null
                val path = it.getString(pathColumn)
                val year = it.getInt(yearColumn)
                val albumId = it.getLong(albumIdColumn)
                val mimeType = it.getString(mimeTypeColumn)
                val discNumber = it.getIntOrNull(discNumberColumn)
                val trackNumber = it.getIntOrNull(trackNumberColumn)

                val artworkUri = Uri.parse("content://media/external/audio/albumart")
                val imgUri = ContentUris.withAppendedId(
                    artworkUri,
                    albumId
                )
                songs.add(MediaItem.Builder()
                    .setUri(Uri.parse(path))
                    .setMediaId(id.toString())
                    .setMimeType(mimeType)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .setAlbumTitle(album)
                            .setAlbumArtist(albumArtist)
                            .setArtworkUri(imgUri)
                            .setTrackNumber(trackNumber)
                            .setDiscNumber(discNumber)
                            .setRecordingYear(year)
                            .setReleaseYear(year)
                            .build()
                    )
                    .build()
                )

                albumMap.getOrPut(Pair(album, year)) { mutableListOf() }.add(songs.last())
                artistMap.getOrPut(artist) { mutableListOf() }.add(songs.last())
            }
        }
        cursor?.close()

        lateinit var sortedAlbumList: MutableList<Album>
        lateinit var sortedArtistList: MutableList<Artist>

        val duration = measureTimeMillis {
            sortedAlbumList = albumMap.entries
                .mapIndexed { index, (key, value) ->
                    val (albumTitle, albumYear) = key
                    val sortedAlbumSongs = value.sortedBy { it.mediaMetadata.trackNumber.toString() }
                    val albumArtist = sortedAlbumSongs.first().mediaMetadata.albumArtist
                        ?: sortedAlbumSongs.first().mediaMetadata.artist.toString()
                    Album(index.toLong(), albumTitle, albumArtist.toString(), albumYear, sortedAlbumSongs)
                }
                .sortedWith(compareBy({ it.title }, { it.albumYear }))
                .toMutableList()
            sortedArtistList = artistMap.entries
                .mapIndexed { index, (artistName, songsByArtist) ->
                    val sortedArtistSongs = songsByArtist.sortedBy { it.mediaMetadata.title.toString() }
                    Artist(index.toLong(), artistName, sortedArtistSongs)
                }
                .sortedBy { it.title }
                .toMutableList()

        }
        Log.d("TAGTAG", "Time elapsed: $duration")

        return Triple(songs, sortedAlbumList, sortedArtistList)
    }

    /**
     * Helper function to insert an element into the sorted position in a list.
     * @param element The element to be inserted.
     * @param comparator The comparator function to compare elements.
     */
    private fun <T> MutableList<T>.insertSorted(element: T, comparator: Comparator<T>) {
        val insertionIndex = this.binarySearch(element, comparator)

        if (insertionIndex < 0) {
            this.add(-(insertionIndex + 1), element)
        } else {
            this.add(insertionIndex, element)
        }
    }

}