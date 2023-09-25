package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SupportComparator
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

class ArtistDecorAdapter(
    context: Context,
    artistCount: Int,
    artistAdapter: ArtistAdapter,
    private val prefs: SharedPreferences
) : BaseDecorAdapter<ArtistAdapter, MediaStoreUtils.Artist>(context, artistCount, artistAdapter) {
    override val pluralStr = R.plurals.artists
    private val viewModel: LibraryViewModel by (context as MainActivity).viewModels()

    override fun onSortButtonPressed(popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.sort_menu_artist_only)
        popupMenu.menu.findItem(R.id.album_artist).isChecked =
            prefs.getBoolean("isDisplayingAlbumArtist", false)

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

                R.id.album_artist -> {
                    menuItem.isChecked = !menuItem.isChecked
                    if (!prefs.getBoolean("isDisplayingAlbumArtist", false)) {
                        prefs.edit().putBoolean("isDisplayingAlbumArtist", true).apply()
                        var itemCount = 0
                        viewModel.albumArtistItemList.value?.let { it1 ->
                            adapter.updateList(
                                it1
                            )
                            itemCount = it1.size
                        }
                        updateSongCounter(itemCount)
                        adapter.setClickEventToAlbumArtist(false)
                    } else {
                        prefs.edit().putBoolean("isDisplayingAlbumArtist", false).apply()
                        var itemCount = 0
                        menuItem.isChecked = !menuItem.isChecked
                        viewModel.artistItemList.value?.let { it1 ->
                            adapter.updateList(
                                it1
                            )
                            itemCount = it1.size
                        }
                        updateSongCounter(itemCount)
                        adapter.setClickEventToAlbumArtist(true)
                    }
                }
            }
            true
        }
    }

    fun updateListToAlbumArtist() {
        prefs.edit().putBoolean("isDisplayingAlbumArtist", true).apply()
        var itemCount = 0
        viewModel.albumArtistItemList.value?.let { it1 ->
            adapter.updateList(
                it1
            )
            itemCount = it1.size
        }
        updateSongCounter(itemCount)
        adapter.setClickEventToAlbumArtist(false)
    }
}
