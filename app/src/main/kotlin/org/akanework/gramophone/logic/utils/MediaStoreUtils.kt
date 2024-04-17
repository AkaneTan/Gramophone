/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.logic.utils

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getColumnIndexOrNull
import org.akanework.gramophone.logic.hasAlbumArtistIdInMediaStore
import org.akanework.gramophone.logic.hasImprovedMediaStore
import org.akanework.gramophone.logic.hasScopedStorageV1
import org.akanework.gramophone.logic.hasScopedStorageV2
import org.akanework.gramophone.logic.hasScopedStorageWithMediaTypes
import org.akanework.gramophone.logic.putIfAbsentSupport
import org.akanework.gramophone.ui.LibraryViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.PriorityQueue

/**
 * [MediaStoreUtils] contains all the methods for reading
 * from mediaStore.
 */
object MediaStoreUtils {

    private const val TAG = "MediaStoreUtils"
    private const val DEBUG_MISSING_SONG = false

    interface Item {
        val id: Long?
        val title: String?
        val songList: MutableList<MediaItem>
    }

    /**
     * [Album] stores Album metadata.
     */
    interface Album : Item {
        override val id: Long?
        override val title: String?
        val artist: String?
        val artistId: Long?
        val albumYear: Int?
        val cover: Uri?
        override val songList: MutableList<MediaItem>
    }

    private data class AlbumImpl(
        override val id: Long?,
        override val title: String?,
        override val artist: String?,
        override var artistId: Long?,
        override val albumYear: Int?,
        override var cover: Uri?,
        override val songList: MutableList<MediaItem>
    ) : Album

    /**
     * [Artist] stores Artist metadata.
     */
    data class Artist(
        override val id: Long?,
        override val title: String?,
        override val songList: MutableList<MediaItem>,
        val albumList: MutableList<Album>,
    ) : Item

    /**
     * [Genre] stores Genre metadata.
     */
    data class Genre(
        override val id: Long?,
        override val title: String?,
        override val songList: MutableList<MediaItem>,
    ) : Item

    /**
     * [Date] stores Date metadata.
     */
    data class Date(
        override val id: Long,
        override val title: String?,
        override val songList: MutableList<MediaItem>,
    ) : Item

    /**
     * [Playlist] stores playlist information.
     */
    open class Playlist(
        override val id: Long,
        override val title: String?,
        override val songList: MutableList<MediaItem>
    ) : Item

    @Parcelize
    data class Lyric(
        val timeStamp: Long? = null,
        val content: String = "",
        var isTranslation: Boolean = false
    ) : Parcelable

    class RecentlyAdded(minAddDate: Long, songList: PriorityQueue<Pair<Long, MediaItem>>) : Playlist(
        -1, null, mutableListOf()
    ) {
        private val rawList: PriorityQueue<Pair<Long, MediaItem>> = songList
        private var filteredList: List<MediaItem>? = null
        var minAddDate: Long = minAddDate
            set(value) {
                if (field != value) {
                    field = value
                    filteredList = null
                }
            }
        override val songList: MutableList<MediaItem>
            get() {
                if (filteredList == null) {
                    val queue = PriorityQueue(rawList)
                    filteredList = mutableListOf<MediaItem>().also {
                        while (!queue.isEmpty()) {
                            val item = queue.poll()!!
                            if (item.first < minAddDate) return@also
                            it.add(item.second)
                        }
                    }
                }
                return filteredList!!.toMutableList()
            }
    }

    /**
     * [LibraryStoreClass] collects above metadata classes
     * together for more convenient reading/writing.
     */
    class LibraryStoreClass(
        val songList: MutableList<MediaItem>,
        val albumList: MutableList<Album>,
        val albumArtistList: MutableList<Artist>,
        val artistList: MutableList<Artist>,
        val genreList: MutableList<Genre>,
        val dateList: MutableList<Date>,
        val playlistList: MutableList<Playlist>,
        val folderStructure: FileNode,
        val shallowFolder: FileNode,
        val folders: Set<String>
    )

    class FileNode(
        val folderName: String
    ) {
        val folderList = hashMapOf<String, FileNode>()
        val songList = mutableListOf<MediaItem>()
        var albumId: Long? = null
            private set
        fun addSong(item: MediaItem, id: Long?) {
            if (albumId != null && id != albumId) {
                albumId = null
            } else if (albumId == null && songList.isEmpty()) {
                albumId = id
            }
            songList.add(item)
        }
    }

    private fun handleMediaFolder(path: String, rootNode: FileNode): FileNode {
        val newPath = if (path.endsWith('/')) path.substring(1, path.length - 1)
        else path.substring(1)
        val splitPath = newPath.split('/')
        var node: FileNode = rootNode
        for (fld in splitPath.subList(0, splitPath.size - 1)) {
            var newNode = node.folderList[fld]
            if (newNode == null) {
                newNode = FileNode(fld)
                node.folderList[newNode.folderName] = newNode
            }
            node = newNode
        }
        return node
    }

    private fun handleShallowMediaItem(
        mediaItem: MediaItem,
        albumId: Long?,
        path: String,
        shallowFolder: FileNode,
        folderArray: MutableList<String>
    ) {
        val newPath = if (path.endsWith('/')) path.substring(0, path.length - 1) else path
        val splitPath = newPath.split('/')
        if (splitPath.size < 2) throw IllegalArgumentException("splitPath.size < 2: $newPath")
        val lastFolderName = splitPath[splitPath.size - 2]
        var folder = shallowFolder.folderList[lastFolderName]
        if (folder == null) {
            folder = FileNode(lastFolderName)
            shallowFolder.folderList[folder.folderName] = folder
            // hack to cut off /
            folderArray.add(
                newPath.substring(0, splitPath[splitPath.size - 1].length + 1)
            )
        }
        folder.addSong(mediaItem, albumId)
    }

    /**
     * [getAllSongs] gets all of your songs from your local disk.
     *
     * @param context
     * @return
     */
    private fun getAllSongs(context: Context): LibraryStoreClass {
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0" +
                listOf(
                    "audio/x-wav",
                    "audio/ogg",
                    "audio/aac",
                    "audio/midi"
                ).joinToString("") { " or ${MediaStore.Audio.Media.MIME_TYPE} = '$it'" }
        val projection =
            arrayListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED
            ).apply {
                if (hasImprovedMediaStore()) {
                    add(MediaStore.Audio.Media.GENRE)
                    add(MediaStore.Audio.Media.GENRE_ID)
                    add(MediaStore.Audio.Media.CD_TRACK_NUMBER)
                    add(MediaStore.Audio.Media.COMPILATION)
                    add(MediaStore.Audio.Media.COMPOSER)
                    add(MediaStore.Audio.Media.DATE_TAKEN)
                    add(MediaStore.Audio.Media.WRITER)
                    add(MediaStore.Audio.Media.DISC_NUMBER)
                    add(MediaStore.Audio.Media.AUTHOR)
                }
            }.toTypedArray()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val limitValue = prefs.getInt(
            "mediastore_filter",
            context.resources.getInteger(R.integer.filter_default_sec)
        )
        val haveImgPerm = if (hasScopedStorageWithMediaTypes())
            context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED else
            prefs.getBoolean("album_covers", false)
        val coverUri = Uri.parse("content://media/external/audio/albumart")
        val folderFilter = prefs.getStringSet("folderFilter", setOf()) ?: setOf()

        // Initialize list and maps.
        val coverCache = if (haveImgPerm) hashMapOf<Long, Pair<File, FileNode>>() else null
        val folders = hashSetOf<String>()
        val folderArray = mutableListOf<String>()
        val root = FileNode("storage")
        val shallowRoot = FileNode("shallow")
        val songs = mutableListOf<MediaItem>()
        val albumMap = hashMapOf<Long?, AlbumImpl>()
        val artistMap = hashMapOf<Long?, Artist>()
        val artistCacheMap = hashMapOf<String?, Long?>()
        val albumArtistMap = hashMapOf<String?, Pair<MutableList<Album>, MutableList<MediaItem>>>()
        // Note: it has been observed on a user's Pixel(!) that MediaStore assigned 3 different IDs
        // for "Unknown genre" (null genre tag), hence we practically ignore genre IDs as key
        val genreMap = hashMapOf<String?, Genre>()
        val dateMap = hashMapOf<Int?, Date>()
        val playlists = mutableListOf<Pair<Playlist, MutableList<Long>>>()
        var foundFavourites = false
        var foundPlaylistContent = false
        val albumIdToArtistMap = if (hasAlbumArtistIdInMediaStore()) {
            val map = hashMapOf<Long, Pair<Long, String?>>()
            context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, arrayOf(
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ARTIST_ID
                ), null, null, null
            )?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                while (it.moveToNext()) {
                    val artistId = it.getLongOrNull(artistIdColumn)
                    if (artistId != null) {
                        val id = it.getLong(idColumn)
                        val artistName = it.getStringOrNull(artistColumn)?.ifEmpty { null }
                        map[id] = Pair(artistId, artistName)
                    }
                }
            }
            map
        } else null
        context.contentResolver.query(@Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, arrayOf(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID,
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME
            ), null, null, null)?.use {
            val playlistIdColumn = it.getColumnIndexOrThrow(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists._ID
            )
            val playlistNameColumn = it.getColumnIndexOrThrow(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME
            )
            while (it.moveToNext()) {
                val playlistId = it.getLong(playlistIdColumn)
                val playlistName = it.getString(playlistNameColumn)?.ifEmpty { null }.run {
                    if (!foundFavourites && this == "gramophone_favourite") {
                        foundFavourites = true
                        context.getString(R.string.playlist_favourite)
                    } else this
                }
                val content = mutableListOf<Long>()
                context.contentResolver.query(
                    @Suppress("DEPRECATION") MediaStore.Audio
                        .Playlists.Members.getContentUri("external", playlistId), arrayOf(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    ), null, null, @Suppress("DEPRECATION")
                    MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
                )?.use { cursor ->
                    val column = cursor.getColumnIndexOrThrow(
                        @Suppress("DEPRECATION") MediaStore.Audio.Playlists.Members.AUDIO_ID
                    )
                    while (cursor.moveToNext()) {
                        foundPlaylistContent = true
                        content.add(cursor.getLong(column))
                    }
                }
                val playlist = Playlist(playlistId, playlistName, mutableListOf())
                playlists.add(Pair(playlist, content))
            }
        }
        val idMap = if (foundPlaylistContent) hashMapOf<Long, MediaItem>() else null
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " COLLATE UNICODE ASC",
        )
        val recentlyAddedMap = PriorityQueue<Pair<Long, MediaItem>>(
            // PriorityQueue throws if initialCapacity < 1
            (cursor?.count ?: 1).coerceAtLeast(1),
            Comparator { a, b ->
                // reversed int order to sort from most recent to least recent
                return@Comparator if (a.first == b.first) 0 else (if (a.first > b.first) -1 else 1)
            })
        cursor?.use {
            // Get columns from mediaStore.
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val discNumberColumn = it.getColumnIndexOrNull(MediaStore.Audio.Media.DISC_NUMBER)
            val trackNumberColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genreColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE) else null
            val genreIdColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_ID) else null
            val cdTrackNumberColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.CD_TRACK_NUMBER) else null
            val compilationColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPILATION) else null
            val dateTakenColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_TAKEN) else null
            val composerColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER) else null
            val writerColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.WRITER) else null
            val authorColumn = if (hasImprovedMediaStore())
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.AUTHOR) else null
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val modifiedDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val path = it.getStringOrNull(pathColumn) ?: continue
                val duration = it.getLongOrNull(durationColumn)
                val pathFile = File(path)
                val fldPath = pathFile.parentFile!!.absolutePath
                val skip = (duration != null && duration < limitValue * 1000) || folderFilter.contains(fldPath)
                // We need to add blacklisted songs to idMap as they can be referenced by playlist
                if (skip && !foundPlaylistContent) continue
                val id = it.getLongOrNull(idColumn)!!
                val title = it.getStringOrNull(titleColumn)!!
                val artist = it.getStringOrNull(artistColumn)
                    .let { v -> if (v == "<unknown>") null else v }
                val album = it.getStringOrNull(albumColumn)
                val albumArtist = it.getStringOrNull(albumArtistColumn)
                val year = it.getIntOrNull(yearColumn).let { v -> if (v == 0) null else v }
                val albumId = it.getLongOrNull(albumIdColumn)
                val artistId = it.getLongOrNull(artistIdColumn)
                val mimeType = it.getStringOrNull(mimeTypeColumn)
                var discNumber = discNumberColumn?.let { col -> it.getIntOrNull(col) }
                var trackNumber = it.getIntOrNull(trackNumberColumn)
                val cdTrackNumber = cdTrackNumberColumn?.let { col -> it.getStringOrNull(col) }
                val compilation = compilationColumn?.let { col -> it.getStringOrNull(col) }
                val dateTaken = dateTakenColumn?.let { col -> it.getStringOrNull(col) }
                val composer = composerColumn?.let { col -> it.getStringOrNull(col) }
                val writer = writerColumn?.let { col -> it.getStringOrNull(col) }
                val author = authorColumn?.let { col -> it.getStringOrNull(col) }
                val genre = genreColumn?.let { col -> it.getStringOrNull(col) }
                val genreId = genreIdColumn?.let { col -> it.getLongOrNull(col) }
                val addDate = it.getLongOrNull(addDateColumn)
                val modifiedDate = it.getLongOrNull(modifiedDateColumn)
                val dateTakenParsed = if (hasImprovedMediaStore()) {
                    // the column exists since R, so we can always use these APIs
                    dateTaken?.toLongOrNull()?.let { it1 -> Instant.ofEpochMilli(it1) }
                        ?.atZone(ZoneId.systemDefault())
                } else null
                val dateTakenYear = if (hasImprovedMediaStore()) {
                    dateTakenParsed?.year
                } else null
                val dateTakenMonth = if (hasImprovedMediaStore()) {
                    dateTakenParsed?.monthValue
                } else null
                val dateTakenDay = if (hasImprovedMediaStore()) {
                    dateTakenParsed?.dayOfMonth
                } else null

                // Since we're using glide, we can get album cover with a uri.
                val imgUri = ContentUris.appendId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon(), id)
                    .appendPath("albumart").build()

                // Process track numbers that have disc number added on.
                // e.g. 1001 - Disc 01, Track 01
                if (trackNumber != null && trackNumber >= 1000) {
                    discNumber = trackNumber / 1000
                    trackNumber %= 1000
                }

                // Build our mediaItem.
                val song = MediaItem
                    .Builder()
                    .setUri(pathFile.toUri())
                    .setMediaId(id.toString())
                    .setMimeType(mimeType)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setTitle(title)
                            .setWriter(writer)
                            .setCompilation(compilation)
                            .setComposer(composer)
                            .setArtist(artist)
                            .setAlbumTitle(album)
                            .setAlbumArtist(albumArtist)
                            .setArtworkUri(imgUri)
                            .setTrackNumber(trackNumber)
                            .setDiscNumber(discNumber)
                            .setGenre(genre)
                            .setRecordingDay(dateTakenDay)
                            .setRecordingMonth(dateTakenMonth)
                            .setRecordingYear(dateTakenYear)
                            .setReleaseYear(year)
                            .setExtras(Bundle().apply {
                                if (artistId != null) {
                                    putLong("ArtistId", artistId)
                                }
                                if (albumId != null) {
                                    putLong("AlbumId", albumId)
                                }
                                if (genreId != null) {
                                    putLong("GenreId", genreId)
                                }
                                putString("Author", author)
                                if (addDate != null) {
                                    putLong("AddDate", addDate)
                                }
                                if (duration != null) {
                                    putLong("Duration", duration)
                                }
                                if (modifiedDate != null) {
                                    putLong("ModifiedDate", modifiedDate)
                                }
                                cdTrackNumber?.toIntOrNull()
                                    ?.let { it1 -> putInt("CdTrackNumber", it1) }
                            })
                            .build(),
                    ).build()
                // Build our metadata maps/lists.
                idMap?.put(id, song)
                // Now that the song can be found by playlists, do NOT register other metadata.
                if (skip) continue
                songs.add(song)
                if (addDate != null) {
                    recentlyAddedMap.add(Pair(addDate, song))
                }
                artistMap.getOrPut(artistId) {
                    Artist(artistId, artist, mutableListOf(), mutableListOf())
                }.songList.add(song)
                artistCacheMap.putIfAbsentSupport(artist, artistId)
                albumMap.getOrPut(albumId) {
                    // in haveImgPerm case, cover uri is created later using coverCache
                    val cover = if (haveImgPerm || albumId == null) null else
                        ContentUris.withAppendedId(coverUri, albumId)
                    val artistStr = albumArtist ?: artist
                    val likelyArtist = albumIdToArtistMap?.get(albumId)
                        ?.run { if (second == artistStr) this else null }
                    AlbumImpl(albumId, album, artistStr, likelyArtist?.first, year, cover, mutableListOf()).also { alb ->
                        albumArtistMap.getOrPut(artistStr) { Pair(mutableListOf(), mutableListOf()) }
                            .first.add(alb)
                    }
                }.also { alb ->
                    albumArtistMap.getOrPut(alb.artist) {
                        Pair(mutableListOf(), mutableListOf()) }.second.add(song)
                }.songList.add(song)
                genreMap.getOrPut(genre) { Genre(genreId, genre, mutableListOf()) }.songList.add(song)
                dateMap.getOrPut(year) { Date(year?.toLong() ?: 0, year?.toString(), mutableListOf()) }.songList.add(song)
                val fn = handleMediaFolder(path, root)
                fn.addSong(song, albumId)
                if (albumId != null) {
                    coverCache?.putIfAbsentSupport(albumId, Pair(pathFile.parentFile!!, fn))
                }
                handleShallowMediaItem(song, albumId, path, shallowRoot, folderArray)
                folders.add(fldPath)
            }
        }

        // Parse all the lists.
        val allowedCoverExtensions = listOf("jpg", "png", "jpeg", "bmp", "tiff", "tif", "webp")
        val albumList = albumMap.values.onEach {
            if (it.artistId == null) {
                it.artistId = artistCacheMap[it.artist]
            }
            artistMap[it.artistId]?.albumList?.add(it)
            // coverCache == null if !haveImgPerm
            coverCache?.get(it.id)?.let { p ->
                // if this is false, folder contains >1 albums
                if (p.second.albumId == it.id) {
                    var bestScore = 0
                    var bestFile: File? = null
                    try {
                        val files = p.first.listFiles() ?: return@let
                        for (file in files) {
                            if (file.extension !in allowedCoverExtensions)
                                continue
                            var score = 1
                            when (file.extension) {
                                "jpg" -> score += 3
                                "png" -> score += 2
                                "jpeg" -> score += 1
                            }
                            if (file.nameWithoutExtension == "albumart") score += 24
                            else if (file.nameWithoutExtension == "cover") score += 20
                            else if (file.nameWithoutExtension.startsWith("albumart")) score += 16
                            else if (file.nameWithoutExtension.startsWith("cover")) score += 12
                            else if (file.nameWithoutExtension.contains("albumart")) score += 8
                            else if (file.nameWithoutExtension.contains("cover")) score += 4
                            if (bestScore < score) {
                                bestScore = score
                                bestFile = file
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, Log.getStackTraceString(e))
                    }
                    // allow .jpg or .png files with any name, but only permit more exotic
                    // formats if name contains either cover or albumart
                    if (bestScore >= 3) {
                        bestFile?.let { f -> it.cover = f.toUri() }
                    }
                }
            }
        }.toMutableList<Album>()
        val artistList = artistMap.values.toMutableList()
        val albumArtistList = albumArtistMap.entries.map { (artist, albumsAndSongs) ->
            Artist(artistCacheMap[artist], artist, albumsAndSongs.second, albumsAndSongs.first)
        }.toMutableList()
        val genreList = genreMap.values.toMutableList()
        val dateList = dateMap.values.toMutableList()
        val playlistsFinal = playlists.map {
            it.first.also { playlist ->
                playlist.songList.addAll(it.second.map { value -> idMap!![value]
                    ?: if (DEBUG_MISSING_SONG) throw NullPointerException(
                        "didn't find song for id $value (playlist ${playlist.title}) in map" +
                                " with ${idMap.size} entries")
                    else dummyMediaItem(value, /* song that does not exist? */"didn't find" +
                            "song for id $value in map with ${idMap.size} entries") })
            }
        }.toMutableList()
        if (!foundFavourites) {
            val values = ContentValues()
            values.put(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.NAME,
                "gramophone_favourite"
            )
            values.put(
                @Suppress("DEPRECATION") MediaStore.Audio.Playlists.DATE_ADDED,
                System.currentTimeMillis()
            )
            values.put(
                @Suppress("DEPRECATION")
                MediaStore.Audio.Playlists.DATE_MODIFIED,
                System.currentTimeMillis()
            )
            val favPlaylistUri =
                context.contentResolver.insert(
                    @Suppress("DEPRECATION") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    values
                )
            if (favPlaylistUri != null) {
                val playlistId = favPlaylistUri.lastPathSegment!!.toLong()
                playlistsFinal.add(
                    Playlist(
                        playlistId,
                        context.getString(R.string.playlist_favourite),
                        mutableListOf()
                    )
                )
            }
        }
        playlistsFinal.add(RecentlyAdded(
            // TODO setting?
            (System.currentTimeMillis() / 1000) - (2 * 7 * 24 * 60 * 60),
            recentlyAddedMap
        ))
        folders.addAll(folderFilter)
        return LibraryStoreClass(
            songs,
            albumList,
            albumArtistList,
            artistList,
            genreList,
            dateList,
            playlistsFinal,
            root,
            shallowRoot,
            folders
        )
    }

    fun updateLibraryWithInCoroutine(libraryViewModel: LibraryViewModel, context: Context, then: (() -> Unit)?) {
        val pairObject = getAllSongs(context)
        CoroutineScope(Dispatchers.Main).launch {
            libraryViewModel.mediaItemList.value = pairObject.songList
            libraryViewModel.albumItemList.value = pairObject.albumList
            libraryViewModel.artistItemList.value = pairObject.artistList
            libraryViewModel.albumArtistItemList.value = pairObject.albumArtistList
            libraryViewModel.genreItemList.value = pairObject.genreList
            libraryViewModel.dateItemList.value = pairObject.dateList
            libraryViewModel.playlistList.value = pairObject.playlistList
            libraryViewModel.folderStructure.value = pairObject.folderStructure
            libraryViewModel.shallowFolderStructure.value = pairObject.shallowFolder
            libraryViewModel.allFolderSet.value = pairObject.folders
            then?.let { it() }
        }
    }

    private fun dummyMediaItem(id: Long, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setTitle(title)
                    .build()
            ).build()
    }

    fun deleteSong(context: Context, item: MediaItem):
            Pair<Boolean, () -> (() -> Pair<IntentSender?, () -> Boolean>)> {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri("external"), item.mediaId.toLong())
        val selector = "${MediaStore.Images.Media._ID} = ?"
        val id = arrayOf(item.mediaId)
        if (hasScopedStorageV2() && context.checkUriPermission(
                uri, Binder.getCallingPid(), Binder.getCallingUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            return Pair(false) {
                val pendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver, listOf(uri)
                )
                return@Pair {
                    Pair(pendingIntent.intentSender) { true }
                }
            }
        } else {
            return Pair(true) {
                try {
                    /**
                     * In [Build.VERSION_CODES.Q], it isn't possible to modify
                     * or delete items in MediaStore directly, and explicit permission
                     * must usually be obtained to do this.
                     *
                     * The way it works is the OS will throw a [RecoverableSecurityException],
                     * which we can catch here. Inside there's an IntentSender which the
                     * activity can use to prompt the user to grant permission to the item
                     * so it can be either updated or deleted.
                     */
                    val result = context.contentResolver.delete(uri, selector, id) == 1
                    return@Pair { Pair(null) { result } }
                } catch (securityException: SecurityException) {
                    return@Pair if (hasScopedStorageV1() &&
                        securityException is RecoverableSecurityException
                    ) {
                        {
                            Pair(securityException.userAction.actionIntent.intentSender) {
                                val res = deleteSong(context, item)
                                val res2 = if (res.first) {
                                    res.second()()
                                } else null
                                if (res2 != null && res2.first == null) {
                                    res2.second()
                                } else {
                                    if (res2 == null) {
                                        Log.e(TAG, "Deleting song failed because uri permission still not granted")
                                    } else {
                                        Log.e(TAG, "Deleting song failed because it threw RecoverableSecurityException")
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.delete_failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    false
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Deleting song failed because: "
                                + Log.getStackTraceString(securityException));
                        {
                            Toast.makeText(
                                context,
                                context.getString(R.string.delete_failed),
                                Toast.LENGTH_LONG
                            ).show()
                            Pair(null) { false }
                        }
                    }
                }
            }
        }
    }

}
