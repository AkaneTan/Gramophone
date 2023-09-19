package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.SupportComparator

class AlbumDecorAdapter(
    private val context: Context,
    private var albumCount: Int,
    private val albumAdapter: AlbumAdapter,
) : RecyclerView.Adapter<AlbumDecorAdapter.ViewHolder>() {
    private var sortStatus = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.general_decor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val songText =
            albumCount.toString() + ' ' +
                if (albumCount <= 1) context.getString(R.string.album) else context.getString(R.string.albums)
        holder.songCounter.text = songText
        holder.sortButton.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.inflate(R.menu.sort_menu_songs)
            popupMenu.menu.findItem(R.id.album).isVisible = false

            when (sortStatus) {
                0 -> {
                    popupMenu.menu.findItem(R.id.name).isChecked = true
                }

                1 -> {
                    popupMenu.menu.findItem(R.id.artist).isChecked = true
                }
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.name -> {
                        if (!menuItem.isChecked) {
                            albumAdapter.sort(SupportComparator.createAlphanumericComparator { it.title })
                            menuItem.isChecked = true
                            sortStatus = 0
                        }
                    }

                    R.id.artist -> {
                        if (!menuItem.isChecked) {
                            albumAdapter.sort(SupportComparator.createAlphanumericComparator { it.artist })
                            menuItem.isChecked = true
                            sortStatus = 1
                        }
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val sortButton: MaterialButton = view.findViewById(R.id.sort)
        val songCounter: TextView = view.findViewById(R.id.song_counter)
    }

    fun updateSongCounter(count: Int) {
        sortStatus = 0
        albumCount = count
        notifyItemChanged(0)
    }

    fun isCounterEmpty(): Boolean = albumCount == 0
}
