package org.akanework.gramophone.ui.adapters

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.getUri
import org.akanework.gramophone.ui.fragments.GeneralSubFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [SongAdapter] is an adapter for displaying songs.
 */
class SongAdapter(
    private val mainActivity: MainActivity,
    songList: MutableLiveData<MutableList<MediaItem>>?,
    canSort: Boolean,
    helper: Sorter.NaturalOrderHelper<MediaItem>? = null
) : BaseAdapter<MediaItem>(mainActivity, songList,
    if (canSort) Sorter.from(helper) else Sorter.noneSorter(),
    if (canSort)
            (if (helper != null) Sorter.Type.NaturalOrder else Sorter.Type.ByTitleAscending)
    else Sorter.Type.None, R.plurals.songs) {

    constructor(mainActivity: MainActivity,
                    songList: MutableList<MediaItem>,
                    canSort: Boolean,
                    helper: Sorter.NaturalOrderHelper<MediaItem>? = null)
            : this(mainActivity, null, canSort, helper) {
                updateList(songList, now = true, false)
            }

    private val viewModel: LibraryViewModel by mainActivity.viewModels()

    override fun getItemViewType(position: Int): Int {
        return R.layout.adapter_list_card
    }

    override fun titleOf(item: MediaItem): String {
        return item.mediaMetadata.title.toString()
    }

    override fun subTitleOf(item: MediaItem): String {
        return item.mediaMetadata.artist?.toString() ?: context.getString(R.string.unknown_artist)
    }

    override fun coverOf(item: MediaItem): Uri? {
        return item.mediaMetadata.artworkUri
    }

    override fun onClick(item: MediaItem) {
        val mediaController = mainActivity.getPlayer()
        mediaController.setMediaItems(list)
        mediaController.seekToDefaultPosition(list.indexOf(item))
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
                                mainActivity
                                    .supportFragmentManager
                                    .beginTransaction()
                                    .addToBackStack("SUBFRAG")
                                    .hide(mainActivity.supportFragmentManager.fragments[0])
                                    .add(
                                        R.id.container,
                                        GeneralSubFragment().apply {
                                            arguments =
                                                Bundle().apply {
                                                    putInt("Position", positionAlbum)
                                                    putInt("Item", 1)
                                                    putString(
                                                        "Title",
                                                        titleOf(item),
                                                    )
                                                }
                                        },
                                    ).commit()
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
                                mainActivity
                                    .supportFragmentManager
                                    .beginTransaction()
                                    .addToBackStack("SUBFRAG")
                                    .hide(mainActivity.supportFragmentManager.fragments[0])
                                    .add(
                                        R.id.container,
                                        GeneralSubFragment().apply {
                                            arguments =
                                                Bundle().apply {
                                                    putInt("Position", positionArtist)
                                                    putInt("Item", 2)
                                                    putString(
                                                        "Title",
                                                        titleOf(item),
                                                    )
                                                }
                                        },
                                    ).commit()
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

    override fun toId(item: MediaItem): String {
        return item.mediaId
    }
}