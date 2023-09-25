package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.MenuItem
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
        popupMenu.menu.findItem(R.id.album_artist).isVisible = true
        popupMenu.menu.findItem(R.id.album_artist).isChecked =
            prefs.getBoolean("isDisplayingAlbumArtist", false)
    }

    override fun onExtraMenuButtonPressed(popupMenu: PopupMenu, menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
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
                true
            }

            else -> false
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
