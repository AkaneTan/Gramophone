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

package org.akanework.gramophone.ui.adapters

import android.net.Uri
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.LibraryViewModel
import org.akanework.gramophone.ui.MediaControllerViewModel
import org.akanework.gramophone.ui.components.NowPlayingDrawable
import org.akanework.gramophone.ui.fragments.ArtistSubFragment
import org.akanework.gramophone.ui.fragments.DetailDialogFragment
import org.akanework.gramophone.ui.fragments.GeneralSubFragment
import java.util.GregorianCalendar


/**
 * [SongAdapter] is an adapter for displaying songs.
 */
class SongAdapter(
    fragment: Fragment,
    songList: MutableLiveData<List<MediaItem>>?,
    canSort: Boolean,
    helper: Sorter.NaturalOrderHelper<MediaItem>?,
    ownsView: Boolean,
    isSubFragment: Boolean = false,
    allowDiffUtils: Boolean = false,
    rawOrderExposed: Boolean = !isSubFragment,
    fallbackSpans: Int = 1
) : BaseAdapter<MediaItem>
    (
    fragment,
    liveData = songList,
    sortHelper = MediaItemHelper(),
    naturalOrderHelper = if (canSort) helper else null,
    initialSortType = if (canSort)
        (if (helper != null) Sorter.Type.NaturalOrder else
                (if (rawOrderExposed) Sorter.Type.NativeOrder else Sorter.Type.ByTitleAscending))
    else Sorter.Type.None,
    canSort = canSort,
    pluralStr = R.plurals.songs,
    ownsView = ownsView,
    defaultLayoutType = LayoutType.COMPACT_LIST,
    isSubFragment = isSubFragment,
    rawOrderExposed = rawOrderExposed,
    allowDiffUtils = allowDiffUtils,
    fallbackSpans = fallbackSpans
) {

    constructor(
        fragment: Fragment,
        songList: List<MediaItem>,
        canSort: Boolean,
        helper: Sorter.NaturalOrderHelper<MediaItem>?,
        ownsView: Boolean,
        isSubFragment: Boolean = false,
        allowDiffUtils: Boolean = false,
        rawOrderExposed: Boolean = !isSubFragment,
        fallbackSpans: Int = 1
    ) : this(
        fragment,
        null,
        canSort,
        helper,
        ownsView,
        isSubFragment,
        allowDiffUtils,
        rawOrderExposed,
        fallbackSpans
    ) {
        updateList(songList, now = true, false)
    }

    fun getSongList() = list

    fun getActivity() = mainActivity

    private val viewModel: LibraryViewModel by fragment.activityViewModels()
    private val mediaControllerViewModel: MediaControllerViewModel by fragment.activityViewModels()
    private var idToPosMap: HashMap<String, Int>? = null
    private var currentMediaItem: String? = null
        set(value) {
            if (field != value) {
                val oldValue = field
                field = value
                if (idToPosMap != null) {
                    val oldPos = idToPosMap!![oldValue]
                    val newPos = idToPosMap!![value]
                    if (oldPos != null) {
                        notifyItemChanged(oldPos, true)
                    }
                    if (newPos != null) {
                        notifyItemChanged(newPos, true)
                    }
                }
            }
        }
    private var currentIsPlaying: Boolean? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null && currentMediaItem != null) {
                    idToPosMap?.get(currentMediaItem)?.let {
                        notifyItemChanged(it, false)
                    }
                }
            }
        }

    init {
        mediaControllerViewModel.addRecreationalPlayerListener(
            fragment.viewLifecycleOwner.lifecycle) {
            currentMediaItem = it.currentMediaItem?.mediaId
            currentIsPlaying = it.isPlaying
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaItem = mediaItem?.mediaId
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    currentIsPlaying = isPlaying
                }
            }
        }
    }

    override fun onListUpdated() {
        // TODO run this method on a different thread / in advance
        idToPosMap = hashMapOf()
        list.forEachIndexed { i, item -> idToPosMap!![item.mediaId] = i }
    }

    override fun virtualTitleOf(item: MediaItem): String {
        return "null"
    }

    override fun onClick(item: MediaItem) {
        val mediaController = mainActivity.getPlayer()
        mediaController?.apply {
            val songList = getSongList()
            setMediaItems(songList, songList.indexOf(item), C.TIME_UNSET)
            prepare()
            play()
        }
    }

    override fun onMenu(item: MediaItem, popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.more_menu)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = mainActivity.getPlayer()
                    mediaController?.addMediaItem(
                        mediaController.currentMediaItemIndex + 1,
                        item,
                    )
                    true
                }

                R.id.album -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val positionAlbum =
                            viewModel.albumItemList.value?.indexOfFirst {
                                (it.title == item.mediaMetadata.albumTitle) &&
                                        (it.songList.contains(item))
                            }
                        if (positionAlbum != null) {
                            withContext(Dispatchers.Main) {
                                mainActivity.startFragment(GeneralSubFragment()) {
                                    putInt("Position", positionAlbum)
                                    putInt("Item", R.id.album)
                                }
                            }
                        }
                    }
                    true
                }

                R.id.artist -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val positionArtist =
                            viewModel.artistItemList.value?.indexOfFirst {
                                val isMatching =
                                    (it.title == item.mediaMetadata.artist) &&
                                            (it.songList.contains(item))
                                isMatching
                            }
                        if (positionArtist != null) {
                            withContext(Dispatchers.Main) {
                                mainActivity.startFragment(ArtistSubFragment()) {
                                    putInt("Position", positionArtist)
                                    putInt("Item", R.id.artist)
                                }
                            }
                        }
                    }
                    true
                }

                R.id.details -> {
                    /*
                    val rootView = MaterialAlertDialogBuilder(mainActivity)
                        .setView(R.layout.dialog_info_song)
                        .setBackground(drawable)
                        .setNeutralButton(R.string.dismiss) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    rootView.findViewById<TextView>(R.id.title)!!.text = item.mediaMetadata.title
                    rootView.findViewById<TextView>(R.id.artist)!!.text = item.mediaMetadata.artist
                    rootView.findViewById<TextView>(R.id.album)!!.text =
                        item.mediaMetadata.albumTitle
                    if (!item.mediaMetadata.albumArtist.isNullOrBlank()) {
                        rootView.findViewById<TextView>(R.id.album_artist)!!.text =
                            item.mediaMetadata.albumArtist
                    }
                    rootView.findViewById<TextView>(R.id.track_number)!!.text =
                        item.mediaMetadata.trackNumber.toString()
                    rootView.findViewById<TextView>(R.id.disc_number)!!.text =
                        item.mediaMetadata.discNumber.toString()
                    val year = item.mediaMetadata.releaseYear?.toString()
                    if (year != null) {
                        rootView.findViewById<TextView>(R.id.year)!!.text = year
                    }
                    val genre = item.mediaMetadata.genre?.toString()
                    if (genre != null) {
                        rootView.findViewById<TextView>(R.id.genre)!!.text = genre
                    }
                    rootView.findViewById<TextView>(R.id.path)!!.text =
                        item.getFile()?.path
                    rootView.findViewById<TextView>(R.id.mime)!!.text =
                        item.mediaMetadata.extras!!.getString("MimeType")
                    rootView.findViewById<TextView>(R.id.duration)!!.text =
                        convertDurationToTimeStamp(item.mediaMetadata.extras!!.getLong("Duration"))

                     */
                    val position = viewModel.mediaItemList.value?.indexOfFirst {
                        it.mediaId == item.mediaId
                    }
                    mainActivity.startFragment(DetailDialogFragment()) {
                        putInt("Position", position!!)
                    }
                    true
                }

                /*R.id.delete -> {
                    val doDelete: (() -> (() -> Pair<IntentSender?, () -> Boolean>)) -> Unit = { r ->
                        val res = r()()
                        if (res.first == null) {
                            res.second()
                        } else {
                            if (mainActivity.intentSenderAction == null) {
                                mainActivity.intentSenderAction = res.second
                                mainActivity.intentSender.launch(
                                    IntentSenderRequest.Builder(res.first!!).build()
                                )
                            } else {
                                Toast.makeText(
                                    context, context.getString(
                                        R.string.delete_in_progress
                                    ), Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    val res = MediaStoreUtils.deleteSong(context, item)
                    if (res.first) {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.delete)
                            .setMessage(item.mediaMetadata.title)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                doDelete(res.second)
                            }
                            .setNegativeButton(R.string.no) { _, _ -> }
                            .show()
                    } else {
                        doDelete(res.second)
                    }
                    true
                }*/

                /*
				R.id.share -> {
					val builder = ShareCompat.IntentBuilder(mainActivity)
					val mimeTypes = mutableSetOf<String>()
					builder.addStream(viewModel.fileUriList.value?.get(songList[holder.bindingAdapterPosition].mediaId.toLong())!!)
					mimeTypes.add(viewModel.mimeTypeList.value?.get(songList[holder.bindingAdapterPosition].mediaId.toLong())!!)
					builder.setType(mimeTypes.singleOrNull() ?: "audio/*").startChooser()
				 } */
				 */
                else -> false
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            if (payloads.none { it is Boolean && it }) {
                holder.nowPlaying.drawable.level = if (currentIsPlaying == true) 1 else 0
                return
            }
            if (currentMediaItem == null || list[position].mediaId != currentMediaItem) {
                holder.nowPlaying.visibility = View.GONE
                holder.nowPlaying.setImageDrawable(null)
                return
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
            if (currentMediaItem == null || list[position].mediaId != currentMediaItem)
                return
        }
        holder.nowPlaying.setImageDrawable(NowPlayingDrawable()
            .also { it.level = if (currentIsPlaying == true) 1 else 0 })
        holder.nowPlaying.visibility = View.VISIBLE
    }

    class MediaItemHelper(
        types: Set<Sorter.Type> = setOf(
            Sorter.Type.ByTitleDescending, Sorter.Type.ByTitleAscending,
            Sorter.Type.ByArtistDescending, Sorter.Type.ByArtistAscending,
            Sorter.Type.ByAlbumTitleDescending, Sorter.Type.ByAlbumTitleAscending,
            Sorter.Type.ByAlbumArtistDescending, Sorter.Type.ByAlbumArtistAscending,
            Sorter.Type.ByAddDateDescending, Sorter.Type.ByAddDateAscending,
            Sorter.Type.ByReleaseDateDescending, Sorter.Type.ByReleaseDateAscending,
            Sorter.Type.ByModifiedDateDescending, Sorter.Type.ByModifiedDateAscending
        )
    ) : Sorter.Helper<MediaItem>(types) {
        override fun getId(item: MediaItem): String {
            return item.mediaId
        }

        override fun getTitle(item: MediaItem): String {
            return item.mediaMetadata.title.toString()
        }

        override fun getArtist(item: MediaItem): String? {
            return item.mediaMetadata.artist?.toString()
        }

        override fun getAlbumTitle(item: MediaItem): String {
            return item.mediaMetadata.albumTitle?.toString() ?: ""
        }

        override fun getAlbumArtist(item: MediaItem): String {
            return item.mediaMetadata.albumArtist?.toString() ?: ""
        }

        override fun getCover(item: MediaItem): Uri? {
            return item.mediaMetadata.artworkUri
        }

        override fun getAddDate(item: MediaItem): Long {
            return item.mediaMetadata.extras!!.getLong("AddDate")
        }

        override fun getReleaseDate(item: MediaItem): Long {
            return GregorianCalendar((item.mediaMetadata.releaseYear ?: 0) + 1900,
                (item.mediaMetadata.releaseMonth ?: 1) - 1,
                item.mediaMetadata.releaseDay ?: 0, 0, 0, 0)
                .timeInMillis
        }

        override fun getModifiedDate(item: MediaItem): Long {
            return item.mediaMetadata.extras!!.getLong("ModifiedDate")
        }
    }
}