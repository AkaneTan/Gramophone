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
 * [GenreAdapter] is an adapter for displaying artists.
 */
class GenreAdapter(
    private val genreList: MutableList<MediaStoreUtils.Genre>,
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : RecyclerView.Adapter<GenreAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_list_card_larger, parent, false),
        )

    override fun getItemCount(): Int = genreList.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.songTitle.text = genreList[position].title
        val songText =
            genreList[position].songList.size.toString() + ' ' +
                if (genreList[position].songList.size <= 1) {
                    context.getString(R.string.song)
                } else {
                    context.getString(R.string.songs)
                }
        holder.songArtist.text = songText

        Glide
            .with(holder.songCover.context)
            .load(
                genreList[position]
                    .songList
                    .first()
                    .mediaMetadata
                    .artworkUri,
            ).placeholder(R.drawable.ic_default_cover_genre)
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
                                putInt("Item", 3)
                                putString("Title", holder.songTitle.text as String)
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
                            genreList[holder.bindingAdapterPosition].songList,
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

    fun sortBy(selector: (MediaStoreUtils.Genre) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasGenreList = mutableListOf<MediaStoreUtils.Genre>()
            wasGenreList.addAll(genreList)
            // Sorting in the background using coroutines
            genreList.sortBy { selector(it) }

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasGenreList, genreList))
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@GenreAdapter)
            }
        }
    }

    fun sortByDescendingInt(selector: (MediaStoreUtils.Genre) -> Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasGenreList = mutableListOf<MediaStoreUtils.Genre>()
            wasGenreList.addAll(genreList)
            // Sorting in the background using coroutines
            genreList.sortByDescending { selector(it) }

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasGenreList, genreList))
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@GenreAdapter)
            }
        }
    }

    fun updateList(newList: MutableList<MediaStoreUtils.Genre>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(genreList, newList))
        genreList.clear()
        genreList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(
        private val oldList: MutableList<MediaStoreUtils.Genre>,
        private val newList: MutableList<MediaStoreUtils.Genre>,
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
