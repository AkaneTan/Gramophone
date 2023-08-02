package org.akanework.serendipity.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.serendipity.R

/**
 * [SongAdapter] is an adapter for displaying songs.
 */
class SongAdapter(private val songList: MutableList<MediaItem>) :
    RecyclerView.Adapter<SongAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_list_card, parent, false))
    }

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = songList[position].mediaMetadata.title
        holder.songArtist.text = songList[position].mediaMetadata.artist

        Glide.with(holder.songCover.context)
            .load(songList[position].mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
    }

    fun sortBy(selector: (MediaItem) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            // Sorting in the background using coroutines
            songList.sortBy { selector(it) }

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                notifyItemRangeChanged(0, songList.size - 1)
            }
        }
    }

    fun finishList(newList: MutableList<MediaItem>) {
        songList.clear()
        songList.addAll(newList)
        notifyItemRangeInserted(0, songList.size)
    }
}