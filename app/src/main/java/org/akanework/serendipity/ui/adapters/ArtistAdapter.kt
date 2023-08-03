package org.akanework.serendipity.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.serendipity.R
import org.akanework.serendipity.logic.utils.MediaStoreUtils

/**
 * [ArtistAdapter] is an adapter for displaying artists.
 */
class ArtistAdapter(private val artistList: MutableList<MediaStoreUtils.Artist>,
    private val context: Context) :
    RecyclerView.Adapter<ArtistAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_list_card_larger, parent, false))
    }

    override fun getItemCount(): Int = artistList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = artistList[position].title
        val songText = artistList[position].songList.size.toString() + ' ' +
                if (artistList[position].songList.size <= 1)
                    context.getString(R.string.song)
                else
                    context.getString(R.string.songs)
        holder.songArtist.text = songText

        Glide.with(holder.songCover.context)
            .load(artistList[position].songList.first().mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover_artist)
            .into(holder.songCover)

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
    }

    fun sortBy(selector: (MediaStoreUtils.Artist) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            // Sorting in the background using coroutines
            artistList.sortBy { selector(it) }

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                notifyItemRangeChanged(0, artistList.size - 1)
            }
        }
    }

    fun sortByDescendingInt(selector: (MediaStoreUtils.Artist) -> Int) {
        CoroutineScope(Dispatchers.Default).launch {
            // Sorting in the background using coroutines
            artistList.sortByDescending { selector(it) }

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                notifyItemRangeChanged(0, artistList.size - 1)
            }
        }
    }

    fun updateList(newList: MutableList<MediaStoreUtils.Artist>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(artistList, newList))
        artistList.clear()
        artistList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(private val oldList: MutableList<MediaStoreUtils.Artist>, private val newList: MutableList<MediaStoreUtils.Artist>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].songList.containsAll(newList[newItemPosition].songList)
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}