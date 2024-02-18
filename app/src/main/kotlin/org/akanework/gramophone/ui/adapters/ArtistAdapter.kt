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
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.fragments.ArtistSubFragment

/**
 * [ArtistAdapter] is an adapter for displaying artists.
 */
class ArtistAdapter(
    private val mainActivity: MainActivity,
    private val artistList: MutableLiveData<MutableList<MediaStoreUtils.Artist>>,
    private val albumArtists: MutableLiveData<MutableList<MediaStoreUtils.Artist>>,
) : BaseAdapter<MediaStoreUtils.Artist>
    (
    mainActivity,
    liveData = null,
    sortHelper = StoreItemHelper(),
    naturalOrderHelper = null,
    initialSortType = Sorter.Type.ByTitleAscending,
    pluralStr = R.plurals.artists,
    ownsView = true,
    defaultLayoutType = LayoutType.LIST
) {

    override fun virtualTitleOf(item: MediaStoreUtils.Artist): String {
        return context.getString(R.string.unknown_artist)
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    var isAlbumArtist = prefs.getBoolean("isDisplayingAlbumArtist", false)
        private set
    override val defaultCover = R.drawable.ic_default_cover_artist

    init {
        liveData = if (isAlbumArtist) albumArtists else artistList
    }

    override fun onClick(item: MediaStoreUtils.Artist) {
        mainActivity.startFragment(ArtistSubFragment()) {
            putInt("Position", toRawPos(item))
            putInt(
                "Item",
                if (isAlbumArtist)
                    R.id.album_artist
                else
                    R.id.artist
            )
        }
    }

    override fun onMenu(item: MediaStoreUtils.Artist, popupMenu: PopupMenu) {

        popupMenu.inflate(R.menu.more_menu_less)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = mainActivity.getPlayer()
                    mediaController?.addMediaItems(
                        mediaController.currentMediaItemIndex + 1,
                        item.songList,
                    )
                    true
                }

                /*
				R.id.share -> {
					val builder = ShareCompat.IntentBuilder(mainActivity)
					val mimeTypes = mutableSetOf<String>()
					builder.addStream(viewModel.fileUriList.value?.get(songList[holder.bindingAdapterPosition].mediaId.toLong())!!)
					mimeTypes.add(viewModel.mimeTypeList.value?.get(songList[holder.bindingAdapterPosition].mediaId.toLong())!!)
					builder.setType(mimeTypes.singleOrNull() ?: "audio/*").startChooser()
				 } */
				 */
                else -> false
            }
        }
    }

    override fun createDecorAdapter(): BaseDecorAdapter<out BaseAdapter<MediaStoreUtils.Artist>> {
        return ArtistDecorAdapter(this)
    }

    private fun setAlbumArtist(albumArtist: Boolean) {
        isAlbumArtist = albumArtist
        prefs.edit().putBoolean("isDisplayingAlbumArtist", isAlbumArtist).apply()
        if (recyclerView != null)
            liveData?.removeObserver(this)
        liveData = if (isAlbumArtist) albumArtists else artistList
        if (recyclerView != null)
            liveData?.observeForever(this)
    }

    private class ArtistDecorAdapter(
        artistAdapter: ArtistAdapter
    ) : BaseDecorAdapter<ArtistAdapter>(artistAdapter, R.plurals.artists) {

        override fun onSortButtonPressed(popupMenu: PopupMenu) {
            popupMenu.menu.findItem(R.id.album_artist).isVisible = true
            popupMenu.menu.findItem(R.id.album_artist).isChecked = adapter.isAlbumArtist
        }

        override fun onExtraMenuButtonPressed(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.album_artist -> {
                    menuItem.isChecked = !menuItem.isChecked
                    adapter.setAlbumArtist(menuItem.isChecked)
                    true
                }

                else -> false
            }
        }
    }
}