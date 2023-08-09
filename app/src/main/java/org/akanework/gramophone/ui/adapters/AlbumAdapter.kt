package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

class AlbumAdapter(private val albumList: MutableList<MediaStoreUtils.Album>,
    private val fragmentManager: FragmentManager
) :
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

        holder.itemView.setOnClickListener {
            fragmentManager.beginTransaction()
                .addToBackStack("SUBFRAG")
                .replace(R.id.container, GeneralSubFragment().apply {
                    arguments = Bundle().apply { putInt("Position", position) }
                })
                .commit()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
    }

    fun sortBy(selector: (MediaStoreUtils.Album) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            // Sorting in the background using coroutines
            val wasAlbumList = mutableListOf<MediaStoreUtils.Album>()
            wasAlbumList.addAll(albumList)
            albumList.sortBy { selector(it) }
            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasAlbumList, albumList))

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@AlbumAdapter)
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
            oldList[oldItemPosition].id == newList[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}