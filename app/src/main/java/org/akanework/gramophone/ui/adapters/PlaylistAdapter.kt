package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

/**
 * [PlaylistAdapter] is an adapter for displaying artists.
 */
class PlaylistAdapter(
    private val mainActivity: MainActivity,
    playlistList: MutableLiveData<MutableList<MediaStoreUtils.Playlist>>,
) : ItemAdapter<MediaStoreUtils.Playlist>
    (mainActivity, playlistList, Sorter.from(), pluralStr = R.plurals.playlists) {

    override val defaultCover = R.drawable.ic_default_cover_playlist

    override fun titleOf(item: MediaStoreUtils.Playlist): String {
        if (item is MediaStoreUtils.RecentlyAdded) {
            return context.getString(R.string.recently_added)
        }
        return item.title ?: context.getString(R.string.unknown_playlist)
    }

    override fun onClick(item: MediaStoreUtils.Playlist) {
        mainActivity.startFragment(
            GeneralSubFragment().apply {
                arguments =
                    Bundle().apply {
                        putInt("Position", toRawPos(item))
                        putInt("Item", 6)
                    }
            },
        )
    }

    override fun onMenu(item: MediaStoreUtils.Playlist, popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.more_menu_less)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = mainActivity.getPlayer()
                    mediaController.addMediaItems(
                        mediaController.currentMediaItemIndex + 1,
                        item.songList,
                    )
                    true
                }
                else -> false
            }
        }
    }

}
