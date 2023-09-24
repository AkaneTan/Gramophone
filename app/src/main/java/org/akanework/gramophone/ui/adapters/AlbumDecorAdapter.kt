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
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SupportComparator

class AlbumDecorAdapter(
    context: Context,
    albumCount: Int,
    albumAdapter: AlbumAdapter,
) : BaseDecorAdapter<AlbumAdapter, MediaStoreUtils.Album>(context, albumCount, albumAdapter) {
    private var sortStatus = 0


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val songText =
            count.toString() + ' ' +
                if (count <= 1) context.getString(R.string.album) else context.getString(R.string.albums)
        holder.counter.text = songText
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
                            adapter.sort(SupportComparator.createAlphanumericComparator { it.title })
                            menuItem.isChecked = true
                            sortStatus = 0
                        }
                    }

                    R.id.artist -> {
                        if (!menuItem.isChecked) {
                            adapter.sort(SupportComparator.createAlphanumericComparator { it.artist })
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
}
