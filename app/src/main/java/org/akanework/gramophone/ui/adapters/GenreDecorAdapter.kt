package org.akanework.gramophone.ui.adapters

import android.content.Context
import androidx.appcompat.widget.PopupMenu
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SupportComparator

class GenreDecorAdapter(
    context: Context,
    genreCount: Int,
    genreAdapter: GenreAdapter,
) : BaseDecorAdapter<GenreAdapter, MediaStoreUtils.Genre>(context, genreCount, genreAdapter) {
    override val pluralStr = R.plurals.items

    override fun onSortButtonPressed(popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.sort_menu_artist)

        when (adapter.sortType) {
            BaseAdapter.Sorter.Type.ByTitleAscending -> {
                popupMenu.menu.findItem(R.id.name).isChecked = true
            }

            BaseAdapter.Sorter.Type.BySizeDescending -> {
                popupMenu.menu.findItem(R.id.size).isChecked = true
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

                R.id.size -> {
                    if (!menuItem.isChecked) {
                        adapter.sort(BaseAdapter.Sorter.Type.BySizeDescending)
                        menuItem.isChecked = true
                    }
                }
            }
            true
        }
    }
}
