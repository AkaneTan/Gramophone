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

class SongDecorAdapter(
    private val context: Context,
    private var songCount: Int,
    private val songAdapter: SongAdapter,
    private val canSort: Boolean,
) : RecyclerView.Adapter<SongDecorAdapter.ViewHolder>() {
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
            songCount.toString() + ' ' +
                if (songCount <= 1) context.getString(R.string.song) else context.getString(R.string.songs)
        holder.songCounter.text = songText
        if (!canSort) holder.sortButton.visibility = View.GONE else
        holder.sortButton.setOnClickListener {
            val popupMenu = PopupMenu(context, it)
            popupMenu.inflate(R.menu.sort_menu_songs)

            when (sortStatus) {
                0 -> {
                    popupMenu.menu.findItem(R.id.name).isChecked = true
                }

                1 -> {
                    popupMenu.menu.findItem(R.id.artist).isChecked = true
                }

                2 -> {
                    popupMenu.menu.findItem(R.id.album).isChecked = true
                }
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.name -> {
                        if (!menuItem.isChecked) {
                            songAdapter.sort(
                                SupportComparator
                                .createAlphanumericComparator { it2 -> it2.mediaMetadata.title!! })
                            menuItem.isChecked = true
                            sortStatus = 0
                        }
                    }

                    R.id.artist -> {
                        if (!menuItem.isChecked) {
                            songAdapter.sort(
                                SupportComparator
                                .createAlphanumericComparator { it2 -> it2.mediaMetadata.artist!! })
                            menuItem.isChecked = true
                            sortStatus = 1
                        }
                    }

                    R.id.album -> {
                        if (!menuItem.isChecked) {
                            songAdapter.sort(
                                SupportComparator
                                .createAlphanumericComparator { it2 -> it2.mediaMetadata.albumTitle!! })
                            menuItem.isChecked = true
                            sortStatus = 2
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
        songCount = count
        notifyItemChanged(0)
    }

    fun isCounterEmpty(): Boolean = songCount == 0
}
