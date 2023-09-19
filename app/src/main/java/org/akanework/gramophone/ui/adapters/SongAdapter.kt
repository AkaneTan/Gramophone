package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.SupportComparator
import org.akanework.gramophone.logic.utils.getUri
import org.akanework.gramophone.ui.fragments.GeneralSubFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [SongAdapter] is an adapter for displaying songs.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class SongAdapter(
    songList: MutableList<MediaItem>,
    private val mainActivity: MainActivity,
    canSort: Boolean,
) : BaseAdapter<MediaItem>(R.layout.adapter_list_card, songList,
    if (canSort) SupportComparator.createAlphanumericComparator { it.mediaMetadata.title!! } else null) {

    private val viewModel: LibraryViewModel by mainActivity.viewModels()
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.title.text = list[position].mediaMetadata.title
        holder.subTitle.text = list[position].mediaMetadata.artist

        Glide
            .with(holder.songCover.context)
            .load(list[position].mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)

        holder.itemView.setOnClickListener {
            val mediaController = mainActivity.getPlayer()
            mediaController.setMediaItems(list)
            mediaController.seekToDefaultPosition(holder.bindingAdapterPosition)
            mediaController.prepare()
            mediaController.play()
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
                            list[holder.bindingAdapterPosition],
                        )
                    }

                    R.id.album -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            val positionAlbum =
                                viewModel.albumItemList.value?.indexOfFirst {
                                    val currentItem = list[holder.bindingAdapterPosition]
                                    val isMatching =
                                        (it.title == currentItem.mediaMetadata.albumTitle) &&
                                            (it.songList.contains(currentItem))
                                    isMatching
                                }
                            if (positionAlbum != null) {
                                withContext(Dispatchers.Main) {
                                    mainActivity
                                        .supportFragmentManager
                                        .beginTransaction()
                                        .addToBackStack("SUBFRAG")
                                        .replace(
                                            R.id.container,
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
                                    val currentItem = list[holder.bindingAdapterPosition]
                                    val isMatching =
                                        (it.title == currentItem.mediaMetadata.artist) &&
                                            (it.songList.contains(currentItem))
                                    isMatching
                                }
                            if (positionArtist != null) {
                                withContext(Dispatchers.Main) {
                                    mainActivity
                                        .supportFragmentManager
                                        .beginTransaction()
                                        .addToBackStack("SUBFRAG")
                                        .replace(
                                            R.id.container,
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
                        val rootView = MaterialAlertDialogBuilder(mainActivity)
                            .setTitle(R.string.dialog_information)
                            .setView(R.layout.dialog_info_song)
                            .setNeutralButton(R.string.dismiss) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        rootView.findViewById<TextInputEditText>(R.id.title)!!
                            .setText(list[holder.bindingAdapterPosition].mediaMetadata.title)
                        rootView.findViewById<TextInputEditText>(R.id.artist)!!
                            .setText(list[holder.bindingAdapterPosition].mediaMetadata.artist)
                        rootView.findViewById<TextInputEditText>(R.id.album)!!
                            .setText(list[holder.bindingAdapterPosition].mediaMetadata.albumTitle)
                        rootView.findViewById<TextInputEditText>(R.id.album_artist)!!
                            .setText(list[holder.bindingAdapterPosition].mediaMetadata.albumArtist)
                        rootView.findViewById<TextInputEditText>(R.id.track_number)!!
                            .setText(list[holder.bindingAdapterPosition].mediaMetadata.trackNumber.toString())
                        val year = list[holder.bindingAdapterPosition].mediaMetadata.releaseYear.toString()
                        if (year != "0") {
                            rootView.findViewById<TextInputEditText>(R.id.year)!!
                                .setText(year)
                        }
                        val genre = list[holder.bindingAdapterPosition].mediaMetadata.genre.toString()
                        if (genre != "null") {
                            rootView.findViewById<TextInputEditText>(R.id.genre)!!
                                .setText(genre)
                        }
                        rootView.findViewById<TextInputEditText>(R.id.path)!!
                            .setText(list[holder.bindingAdapterPosition].getUri().toString())

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

    override fun toId(item: MediaItem): String {
        return item.mediaId
    }
}
