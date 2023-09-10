package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

/**
 * [ArtistAdapter] is an adapter for displaying artists.
 */
class ArtistAdapter(
    private val artistList: MutableList<MediaStoreUtils.Artist>,
    private val albumArtistList: MutableList<MediaStoreUtils.Artist>,
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : RecyclerView.Adapter<ArtistAdapter.ViewHolder>() {

    private var isAlbumArtist = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_list_card_larger, parent, false),
        )

    override fun getItemCount(): Int = artistList.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.songTitle.text = artistList[position].title
        val songText =
            artistList[position].songList.size.toString() + ' ' +
                if (artistList[position].songList.size <= 1) {
                    context.getString(R.string.song)
                } else {
                    context.getString(R.string.songs)
                }
        holder.songArtist.text = songText

        Glide
            .with(holder.songCover.context)
            .load(
                artistList[position]
                    .songList
                    .first()
                    .mediaMetadata
                    .artworkUri,
            ).placeholder(R.drawable.ic_default_cover_artist)
            .into(holder.songCover)

        holder.itemView.setOnClickListener {
            fragmentManager
                .beginTransaction()
                .addToBackStack("SUBFRAG")
                .replace(
                    R.id.container,
                    GeneralSubFragment().apply {
                        arguments =
                            Bundle().apply {
                                putInt("Position", position)
                                putInt("Item",
                                    if (!isAlbumArtist)
                                        2
                                    else
                                        5)
                                putString("Title",
                                    if (!isAlbumArtist)
                                        artistList[position].title
                                    else
                                        albumArtistList[position].title)
                            }
                    },
                ).commit()
        }

        holder.moreButton.setOnClickListener { it ->
            val popupMenu = PopupMenu(it.context, it)
            popupMenu.inflate(R.menu.more_menu_less)

            popupMenu.setOnMenuItemClickListener { it1 ->
                when (it1.itemId) {
                    R.id.play_next -> {
                        val mediaController = mainActivity.getPlayer()
                        mediaController.addMediaItems(
                            mediaController.currentMediaItemIndex + 1,
                            if (!isAlbumArtist)
                                artistList[holder.bindingAdapterPosition].songList
                            else
                                albumArtistList[holder.bindingAdapterPosition].songList,
                        )
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

    fun sortBy(selector: (MediaStoreUtils.Artist) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasArtistList = mutableListOf<MediaStoreUtils.Artist>()
            wasArtistList.addAll(artistList)

            // Sorting in the background using coroutines
            artistList.sortBy { selector(it) }
            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasArtistList, artistList))

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@ArtistAdapter)
            }
        }
    }

    fun sortByDescendingInt(selector: (MediaStoreUtils.Artist) -> Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasArtistList = mutableListOf<MediaStoreUtils.Artist>()
            wasArtistList.addAll(artistList)

            // Sorting in the background using coroutines
            artistList.sortByDescending { selector(it) }
            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasArtistList, artistList))

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@ArtistAdapter)
            }
        }
    }

    fun updateList(newList: MutableList<MediaStoreUtils.Artist>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(artistList, newList))
        artistList.clear()
        artistList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setClickEventToAlbumArtist(reverse: Boolean = false) {
        isAlbumArtist = !reverse
    }

    private class SongDiffCallback(
        private val oldList: MutableList<MediaStoreUtils.Artist>,
        private val newList: MutableList<MediaStoreUtils.Artist>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
