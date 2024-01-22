/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

/**
 * [PlaylistAdapter] is an adapter for displaying artists.
 */
class PlaylistAdapter(
    private val mainActivity: MainActivity,
    playlistList: MutableLiveData<MutableList<MediaStoreUtils.Playlist>>,
) : BaseAdapter<MediaStoreUtils.Playlist>
    (
    mainActivity,
    liveData = playlistList,
    sortHelper = StoreItemHelper(),
    naturalOrderHelper = null,
    initialSortType = Sorter.Type.ByTitleAscending,
    pluralStr = R.plurals.items,
    ownsView = true,
    defaultLayoutType = LayoutType.LIST
) {

    override val defaultCover = R.drawable.ic_default_cover_playlist

    override fun virtualTitleOf(item: MediaStoreUtils.Playlist): String {
        return context.getString(
            if (item is MediaStoreUtils.RecentlyAdded)
                R.string.recently_added else R.string.unknown_playlist
        )
    }

    override fun onClick(item: MediaStoreUtils.Playlist) {
        mainActivity.startFragment(
            GeneralSubFragment().apply {
                arguments =
                    Bundle().apply {
                        putInt("Position", toRawPos(item))
                        putInt("Item", R.id.playlist)
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
