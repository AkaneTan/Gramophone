/*
 *     Copyright (C) 2023  Akane Foundation
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
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getUri
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.fragments.ArtistSubFragment
import org.akanework.gramophone.ui.fragments.GeneralSubFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel


/**
 * [SongAdapter] is an adapter for displaying songs.
 */
class SongAdapter(
    private val mainActivity: MainActivity,
    songList: MutableLiveData<MutableList<MediaItem>>?,
    canSort: Boolean,
    helper: Sorter.NaturalOrderHelper<MediaItem>?,
    ownsView: Boolean
) : BaseAdapter<MediaItem>
    (
    mainActivity,
    liveData = songList,
    sortHelper = MediaItemHelper(),
    naturalOrderHelper = if (canSort) helper else null,
    initialSortType = if (canSort)
        (if (helper != null) Sorter.Type.NaturalOrder else Sorter.Type.ByTitleAscending)
    else Sorter.Type.None,
    pluralStr = R.plurals.songs,
    ownsView = ownsView,
    defaultLayoutType = LayoutType.COMPACT_LIST,
    indicatorResource = R.drawable.ic_music_note
) {

    constructor(
        mainActivity: MainActivity,
        songList: List<MediaItem>,
        canSort: Boolean,
        helper: Sorter.NaturalOrderHelper<MediaItem>?,
        ownsView: Boolean
    )
            : this(mainActivity, null, canSort, helper, ownsView) {
        updateList(songList, now = true, false)
    }

    private val viewModel: LibraryViewModel by mainActivity.viewModels()

    override fun virtualTitleOf(item: MediaItem): String {
        return "null"
    }

    override fun onClick(item: MediaItem) {
        val mediaController = mainActivity.getPlayer()
        mediaController.setMediaItems(list, list.indexOf(item), C.TIME_UNSET)
        mediaController.prepare()
        mediaController.play()
    }

    override fun onMenu(item: MediaItem, popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.more_menu)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = mainActivity.getPlayer()
                    mediaController.addMediaItem(
                        mediaController.currentMediaItemIndex + 1,
                        item,
                    )
                    true
                }

                R.id.album -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val positionAlbum =
                            viewModel.albumItemList.value?.indexOfFirst {
                                val isMatching =
                                    (it.title == item.mediaMetadata.albumTitle) &&
                                            (it.songList.contains(item))
                                isMatching
                            }
                        if (positionAlbum != null) {
                            withContext(Dispatchers.Main) {
                                mainActivity.startFragment(
                                    GeneralSubFragment().apply {
                                        arguments =
                                            Bundle().apply {
                                                putInt("Position", positionAlbum)
                                                putInt("Item", R.id.album)
                                            }
                                    },
                                )
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
                                mainActivity.startFragment(
                                    ArtistSubFragment().apply {
                                        arguments =
                                            Bundle().apply {
                                                putInt("Position", positionArtist)
                                                putInt("Item", R.id.artist)
                                            }
                                    },
                                )
                            }
                        }
                    }
                    true
                }

                R.id.details -> {
                    val rootView = MaterialAlertDialogBuilder(mainActivity)
                        .setTitle(R.string.dialog_information)
                        .setView(R.layout.dialog_info_song)
                        .setNeutralButton(R.string.dismiss) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    val topDivider = rootView.findViewById<MaterialDivider>(R.id.top_divider)
                    val bottomDivider = rootView.findViewById<MaterialDivider>(R.id.bottom_divider)
                    val nestedScrollView = rootView.findViewById<NestedScrollView>(R.id.nestedscrollview)
                    rootView.findViewById<TextInputEditText>(R.id.title)!!
                        .setText(item.mediaMetadata.title)
                    rootView.findViewById<TextInputEditText>(R.id.artist)!!
                        .setText(item.mediaMetadata.artist)
                    rootView.findViewById<TextInputEditText>(R.id.album)!!
                        .setText(item.mediaMetadata.albumTitle)
                    rootView.findViewById<TextInputEditText>(R.id.album_artist)!!
                        .setText(item.mediaMetadata.albumArtist)
                    rootView.findViewById<TextInputEditText>(R.id.track_number)!!
                        .setText(item.mediaMetadata.trackNumber.toString())
                    rootView.findViewById<TextInputEditText>(R.id.disc_number)!!
                        .setText(item.mediaMetadata.discNumber.toString())
                    if (Build.VERSION.SDK_INT >= 23) {
                        nestedScrollView!!.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                            val bottomDiff: Int =
                                nestedScrollView.getChildAt(
                                    nestedScrollView.childCount - 1
                                ).bottom - (
                                    nestedScrollView.height
                                        + scrollY
                                )
                            if (bottomDiff == 0) {
                                bottomDivider!!.visibility = View.GONE
                                topDivider!!.visibility = View.VISIBLE
                            }

                            if (scrollY == 0) {
                                bottomDivider!!.visibility = View.VISIBLE
                                topDivider!!.visibility = View.GONE
                            }
                        }
                    }
                    val year = item.mediaMetadata.releaseYear?.toString()
                    if (year != null) {
                        rootView.findViewById<TextInputEditText>(R.id.year)!!
                            .setText(year)
                    }
                    val genre = item.mediaMetadata.genre?.toString()
                    if (genre != null) {
                        rootView.findViewById<TextInputEditText>(R.id.genre)!!
                            .setText(genre)
                    }
                    rootView.findViewById<TextInputEditText>(R.id.path)!!
                        .setText(item.getUri().toString())
                    true
                }
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

    class MediaItemHelper(
        types: Set<Sorter.Type> = setOf(
            Sorter.Type.ByTitleDescending, Sorter.Type.ByTitleAscending,
            Sorter.Type.ByArtistDescending, Sorter.Type.ByArtistAscending,
            Sorter.Type.ByAlbumTitleDescending, Sorter.Type.ByAlbumTitleAscending,
            Sorter.Type.ByAlbumArtistDescending, Sorter.Type.ByAlbumArtistAscending
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
    }
}