package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.media3.common.MediaItem
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.SupportComparator

class SongDecorAdapter(
    context: Context,
    songCount: Int,
    songAdapter: SongAdapter,
    canSort: Boolean,
) : BaseDecorAdapter<SongAdapter, MediaItem>
    (context, songCount, songAdapter, canSort) {
    override val pluralStr = R.plurals.songs

    override fun onSortButtonPressed(popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.sort_menu_songs)

        when (adapter.sortType) {
            BaseAdapter.Sorter.Type.ByTitleAscending -> {
                popupMenu.menu.findItem(R.id.name).isChecked = true
            }

            BaseAdapter.Sorter.Type.ByArtistAscending -> {
                popupMenu.menu.findItem(R.id.artist).isChecked = true
            }

            BaseAdapter.Sorter.Type.ByAlbumTitleAscending -> {
                popupMenu.menu.findItem(R.id.album).isChecked = true
            }

            BaseAdapter.Sorter.Type.None -> {}

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

                R.id.album -> {
                    if (!menuItem.isChecked) {
                        adapter.sort(BaseAdapter.Sorter.Type.ByAlbumTitleAscending)
                        menuItem.isChecked = true
                    }
                }
            }
            true
        }
    }
}
