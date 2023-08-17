package org.akanework.gramophone.logic.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.akanework.gramophone.R

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

    data class Genre(
        val id: Long,
        val title: String,
        val songList: List<MediaItem>
    )

    data class Date(
        val id: Long,
        val title: Int,
        val songList: List<MediaItem>
    )

    data class LibraryStoreClass(
        val songList: MutableList<MediaItem>,
        val albumList: MutableList<Album>,
        val artistList: MutableList<Artist>,
        val genreList: MutableList<Genre>,
        val dateList: MutableList<Date>
    )

    /**
     * [getAllSongs] gets all of your songs from your local disk.
     *
     * @param context
     * @return
     */
    fun getAllSongs(context: Context): LibraryStoreClass {
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
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.GENRE
        )
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"

        val songs = mutableListOf<MediaItem>()
        val albumMap = mutableMapOf<Pair<String, Int>, MutableList<MediaItem>>()
        val artistMap = mutableMapOf<String, MutableList<MediaItem>>()
        val genreMap = mutableMapOf<String?, MutableList<MediaItem>>()
        val dateMap = mutableMapOf<Int, MutableList<MediaItem>>()
        val unknownGenre = context.getString(R.string.unknown_genre)
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
            val trackNumberColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genreColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)

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
                var discNumber = it.getInt(discNumberColumn)
                var trackNumber = it.getInt(trackNumberColumn)
                val genre = it.getStringOrNull(genreColumn)

                val artworkUri = Uri.parse("content://media/external/audio/albumart")
                val imgUri = ContentUris.withAppendedId(
                    artworkUri,
                    albumId
                )

                if (trackNumber.toString().length == 4) {
                    discNumber = trackNumber.toString().substring(0, 1).toInt()
                    trackNumber = trackNumber.toString().substring(3, 4).toInt()
                }

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
                genreMap.getOrPut(genre) { mutableListOf() }.add(songs.last())
                dateMap.getOrPut(year) { mutableListOf() }.add(songs.last())
            }
        }
        cursor?.close()


        val sortedAlbumList: MutableList<Album> = albumMap.entries
            .mapIndexed { index, (key, value) ->
                val (albumTitle, albumYear) = key
                val sortedAlbumSongs = value.sortedBy { it.mediaMetadata.trackNumber.toString() }
                val albumArtist = sortedAlbumSongs.first().mediaMetadata.albumArtist
                    ?: sortedAlbumSongs.first().mediaMetadata.artist.toString()
                Album(index.toLong(), albumTitle, albumArtist.toString(), albumYear, sortedAlbumSongs)
            }
            .sortedWith(compareBy({ it.title }, { it.albumYear }))
            .toMutableList()
        val sortedArtistList: MutableList<Artist> = artistMap.entries
            .mapIndexed { index, (artistName, songsByArtist) ->
                val sortedArtistSongs = songsByArtist.sortedBy { it.mediaMetadata.title.toString() }
                Artist(index.toLong(), artistName, sortedArtistSongs)
            }
            .sortedBy { it.title }
            .toMutableList()
        val sortedGenreList: MutableList<Genre> = genreMap.entries
            .mapIndexed { index, (genreTitle, songsByGenre) ->
                val sortedGenreSongs = songsByGenre.sortedBy { it.mediaMetadata.title.toString() }
                Genre(index.toLong(), genreTitle ?: unknownGenre, sortedGenreSongs)
            }
            .sortedBy { it.title }
            .toMutableList()
        val sortedDateList: MutableList<Date> = dateMap.entries
            .mapIndexed { index, (year, songsByYear) ->
                val sortedDateSongs = songsByYear.sortedBy { it.mediaMetadata.title.toString() }
                Date(index.toLong(), year, sortedDateSongs)
            }
            .sortedByDescending { it.title }
            .toMutableList()

        return LibraryStoreClass(songs, sortedAlbumList, sortedArtistList, sortedGenreList, sortedDateList)
    }

}