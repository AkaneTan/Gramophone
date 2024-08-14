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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.hasScopedStorageV1
import org.akanework.gramophone.logic.hasScopedStorageV2
import org.akanework.gramophone.ui.LibraryViewModel
import uk.akane.libphonograph.reader.Reader
import uk.akane.libphonograph.reader.ReaderResult
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
    private fun getAllSongs(context: Context): ReaderResult<MediaItem> {
        return Reader.readFromMediaStore(context,
            { uri, mediaId, mimeType, title, writer, compilation, composer, artist,
                              albumTitle, albumArtist, artworkUri, cdTrackNumber, trackNumber,
                              discNumber, genre, recordingDay, recordingMonth, recordingYear,
                              releaseYear, artistId, albumId, genreId, author, addDate, duration,
                              modifiedDate ->
                return@readFromMediaStore MediaItem
                    .Builder()
                    .setUri(uri)
                    .setMediaId(mediaId.toString())
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
                            .setAlbumTitle(albumTitle)
                            .setAlbumArtist(albumArtist)
                            .setArtworkUri(artworkUri)
                            .setTrackNumber(trackNumber)
                            .setDiscNumber(discNumber)
                            .setGenre(genre)
                            .setRecordingDay(recordingDay)
                            .setRecordingMonth(recordingMonth)
                            .setRecordingYear(recordingYear)
                            .setReleaseYear(releaseYear)
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
            })
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
