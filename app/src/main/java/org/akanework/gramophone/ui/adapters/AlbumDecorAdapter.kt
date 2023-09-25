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
    override val pluralStr = R.plurals.albums

    override fun onSortButtonPressed(popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.sort_menu_songs)
        popupMenu.menu.findItem(R.id.album).isVisible = false

        when (adapter.sortType) {
            BaseAdapter.Sorter.Type.ByTitleAscending -> {
                popupMenu.menu.findItem(R.id.name).isChecked = true
            }

            BaseAdapter.Sorter.Type.ByArtistAscending -> {
                popupMenu.menu.findItem(R.id.artist).isChecked = true
            }

            else -> throw IllegalStateException()
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.name -> {
                    if (!menuItem.isChecked) {
                        adapter.sort(BaseAdapter.Sorter.Type.ByTitleAscending)
                        menuItem.isChecked = true
                    }
                }

                R.id.artist -> {
                    if (!menuItem.isChecked) {
                        adapter.sort(BaseAdapter.Sorter.Type.ByArtistAscending)
                        menuItem.isChecked = true
                    }
                }
            }
            true
        }
    }
}
