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

class PlaylistDecorAdapter(
    context: Context,
    count: Int,
    adapter: PlaylistAdapter,
) : BaseDecorAdapter<PlaylistAdapter, MediaStoreUtils.Playlist>
    (context, count, adapter) {
    override val pluralStr = R.plurals.playlists
    private var sortStatus = 0

    override fun onSortButtonPressed(popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.sort_menu_artist)

        when (sortStatus) {
            0 -> {
                popupMenu.menu.findItem(R.id.name).isChecked = true
            }

            1 -> {
                popupMenu.menu.findItem(R.id.size).isChecked = true
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.name -> {
                    if (!menuItem.isChecked) {
                        adapter.sort(SupportComparator.createAlphanumericComparator { it2 -> it2.title })
                        menuItem.isChecked = true
                        sortStatus = 0
                    }
                }

                R.id.size -> {
                    if (!menuItem.isChecked) {
                        adapter.sort(compareByDescending { it2 -> it2.songList.size })
                        menuItem.isChecked = true
                        sortStatus = 1
                    }
                }
            }
            true
        }
    }
}
