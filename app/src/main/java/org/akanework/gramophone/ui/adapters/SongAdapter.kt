package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.GeneralSubFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [SongAdapter] is an adapter for displaying songs.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class SongAdapter(
    private val songList: MutableList<MediaItem>,
    private val mainActivity: MainActivity,
) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_list_card, parent, false),
        )

    private val viewModel: LibraryViewModel by mainActivity.viewModels()

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.songTitle.text = songList[position].mediaMetadata.title
        holder.songArtist.text = songList[position].mediaMetadata.artist

        Glide
            .with(holder.songCover.context)
            .load(songList[position].mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)

        holder.itemView.setOnClickListener {
            val standardBottomSheet = mainActivity.findViewById<FrameLayout>(R.id.player_layout)
            val standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet)
            val mediaController = mainActivity.getPlayer()
            mediaController.setMediaItems(songList)
            mediaController.seekToDefaultPosition(holder.bindingAdapterPosition)
            mediaController.prepare()
            mediaController.play()
            if (standardBottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                standardBottomSheetBehavior.isHideable = false
                standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        holder.moreButton.setOnClickListener { it ->
            val popupMenu = PopupMenu(it.context, it)
            popupMenu.inflate(R.menu.more_menu)

            popupMenu.setOnMenuItemClickListener { it1 ->
                when (it1.itemId) {
                    R.id.play_next -> {
                        val mediaController = mainActivity.getPlayer()
                        mediaController.addMediaItem(
                            mediaController.currentMediaItemIndex + 1,
                            songList[holder.bindingAdapterPosition],
                        )
                    }

                    R.id.album -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            val positionAlbum =
                                viewModel.albumItemList.value?.indexOfFirst {
                                    val currentItem = songList[holder.bindingAdapterPosition]
                                    val isMatching =
                                        (it.title == currentItem.mediaMetadata.albumTitle) &&
                                            (it.songList.contains(currentItem))
                                    isMatching
                                }
                            if (positionAlbum != null) {
                                withContext(Dispatchers.Main) {
                                    mainActivity
                                        .getPlayerUiFragmentManager()
                                        .beginTransaction()
                                        .addToBackStack("SUBFRAG")
                                        .replace(
                                            R.id.fragment_player_ui,
                                            GeneralSubFragment().apply {
                                                arguments =
                                                    Bundle().apply {
                                                        putInt("Position", positionAlbum)
                                                        putInt("Item", 1)
                                                        putString(
                                                            "Title",
                                                            viewModel
                                                                .albumItemList
                                                                .value
                                                                ?.get(positionAlbum)
                                                                ?.title,
                                                        )
                                                    }
                                            },
                                        ).commit()
                                }
                            }
                        }
                    }

                    R.id.artist -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            val positionArtist =
                                viewModel.artistItemList.value?.indexOfFirst {
                                    val currentItem = songList[holder.bindingAdapterPosition]
                                    val isMatching =
                                        (it.title == currentItem.mediaMetadata.artist) &&
                                            (it.songList.contains(currentItem))
                                    isMatching
                                }
                            if (positionArtist != null) {
                                withContext(Dispatchers.Main) {
                                    mainActivity
                                        .getPlayerUiFragmentManager()
                                        .beginTransaction()
                                        .addToBackStack("SUBFRAG")
                                        .replace(
                                            R.id.fragment_player_ui,
                                            GeneralSubFragment().apply {
                                                arguments =
                                                    Bundle().apply {
                                                        putInt("Position", positionArtist)
                                                        putInt("Item", 2)
                                                        putString(
                                                            "Title",
                                                            viewModel
                                                                .artistItemList
                                                                .value
                                                                ?.get(
                                                                    positionArtist,
                                                                )?.title,
                                                        )
                                                    }
                                            },
                                        ).commit()
                                }
                            }
                        }
                    }

                    R.id.details -> {
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
                }
                true
            }
            popupMenu.show()
        }
    }

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
        val moreButton: MaterialButton = view.findViewById(R.id.more)
    }

    fun sortBy(selector: (MediaItem) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasSongList = mutableListOf<MediaItem>()
            wasSongList.addAll(songList)
            // Sorting in the background using coroutines
            songList.sortBy { selector(it) }

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasSongList, songList))
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@SongAdapter)
            }
        }
    }

    fun updateList(newList: MutableList<MediaItem>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(songList, newList))
        songList.clear()
        songList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(
        private val oldList: List<MediaItem>,
        private val newList: List<MediaItem>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition].mediaId == newList[newItemPosition].mediaId

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
