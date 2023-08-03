package org.akanework.serendipity.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.serendipity.R
import org.akanework.serendipity.logic.utils.MediaStoreUtils

class AlbumAdapter(private val albumList: MutableList<MediaStoreUtils.Album>) :
    RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_grid_card, parent, false))
    }

    override fun getItemCount(): Int = albumList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = albumList[position].title
        holder.songArtist.text = albumList[position].artist

        Glide.with(holder.songCover.context)
            .load(albumList[position].songList.first().mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
    }

    fun sortBy(selector: (MediaStoreUtils.Album) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            // Sorting in the background using coroutines
            albumList.sortBy { selector(it) }

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                notifyItemRangeChanged(0, albumList.size - 1)
            }
        }
    }

    fun updateList(newList: MutableList<MediaStoreUtils.Album>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(albumList, newList))
        albumList.clear()
        albumList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(private val oldList: MutableList<MediaStoreUtils.Album>, private val newList: MutableList<MediaStoreUtils.Album>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            (oldList[oldItemPosition].title + oldList[oldItemPosition].artist) == (newList[newItemPosition].title + newList[newItemPosition].artist)
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}