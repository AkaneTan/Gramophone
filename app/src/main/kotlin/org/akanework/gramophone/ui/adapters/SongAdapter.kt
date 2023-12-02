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

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dp
import org.akanework.gramophone.logic.getUri
import org.akanework.gramophone.logic.utils.CalculationUtils.convertDurationToTimeStamp
import org.akanework.gramophone.logic.utils.ColorUtils
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
    ownsView: Boolean,
    private val isTrackDiscNumAvailable: Boolean = false
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
        ownsView: Boolean,
        isTrackDiscNumAvailable: Boolean = false
    )
            : this(mainActivity, null, canSort, helper, ownsView, isTrackDiscNumAvailable) {
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (isTrackDiscNumAvailable) {
            val targetText =
                list[position].mediaMetadata.trackNumber.toString() +
                " | " + context.resources.getString(R.string.disc) + " " +
                    list[position].mediaMetadata.discNumber
            holder.indicator.text = targetText
        }
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
                    val processColor = ColorUtils.getColor(
                        MaterialColors.getColor(
                            context,
                            android.R.attr.colorBackground,
                            -1
                        ),
                        ColorUtils.ColorType.COLOR_BACKGROUND_ELEVATED,
                        context,
                        true
                    )
                    val drawable = GradientDrawable()
                    drawable.color =
                        ColorStateList.valueOf(processColor)
                    drawable.cornerRadius = 64f
                    val rootView = MaterialAlertDialogBuilder(mainActivity)
                        .setView(R.layout.dialog_info_song)
                        .setBackground(drawable)
                        .setNeutralButton(R.string.dismiss) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    rootView.findViewById<TextView>(R.id.title)!!.text = item.mediaMetadata.title
                    rootView.findViewById<TextView>(R.id.artist)!!.text = item.mediaMetadata.artist
                    rootView.findViewById<TextView>(R.id.album)!!.text = item.mediaMetadata.albumTitle
                    if (!item.mediaMetadata.albumArtist.isNullOrBlank()) {
                        rootView.findViewById<TextView>(R.id.album_artist)!!.text =
                            item.mediaMetadata.albumArtist
                    }
                    rootView.findViewById<TextView>(R.id.track_number)!!.text = item.mediaMetadata.trackNumber.toString()
                    rootView.findViewById<TextView>(R.id.disc_number)!!.text = item.mediaMetadata.discNumber.toString()
                    val year = item.mediaMetadata.releaseYear?.toString()
                    if (year != null) {
                        rootView.findViewById<TextView>(R.id.year)!!.text = year
                    }
                    val genre = item.mediaMetadata.genre?.toString()
                    if (genre != null) {
                        rootView.findViewById<TextView>(R.id.genre)!!.text = genre
                    }
                    rootView.findViewById<TextView>(R.id.path)!!.text = item.getUri().toString()
                    rootView.findViewById<TextView>(R.id.mime)!!.text = item.mediaMetadata.extras!!.getString("MimeType")
                    rootView.findViewById<TextView>(R.id.duration)!!.text = convertDurationToTimeStamp(item.mediaMetadata.extras!!.getLong("Duration"))
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