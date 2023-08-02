package org.akanework.serendipity.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.serendipity.R

class SongDecorAdapter(private val context: Context,
                       private var songCount: Int,
                       private val songAdapter: SongAdapter)
    : RecyclerView.Adapter<SongDecorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_decor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val songText = songCount.toString() + ' '+
                if (songCount <= 1) context.getString(R.string.song) else context.getString(R.string.songs)
        holder.songCounter.text = songText
        holder.sortButton.setOnClickListener {

            val popupMenu = PopupMenu(context, it)
            popupMenu.menu.add(context.getString(R.string.sort_by_name))
            popupMenu.menu.add(context.getString(R.string.sort_by_artist))
            popupMenu.menu.add(context.getString(R.string.sort_by_album))

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    context.getString(R.string.sort_by_name) -> {
                        songAdapter.sortBy { it2 -> it2.mediaMetadata.title.toString() }
                    }
                    context.getString(R.string.sort_by_artist) -> {
                        songAdapter.sortBy { it2 -> it2.mediaMetadata.artist.toString() }
                    }
                    context.getString(R.string.sort_by_album) -> {
                        songAdapter.sortBy { it2 -> it2.mediaMetadata.albumTitle.toString() }
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sortButton: MaterialButton = view.findViewById(R.id.sort)
        val songCounter: TextView = view.findViewById(R.id.song_counter)
    }

    fun updateSongCounter(count: Int) {
        songCount = count
        notifyItemChanged(0)
    }
}
