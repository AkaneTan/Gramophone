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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.AdapterFragment

/**
 * This is the ViewPager2 adapter.
 */
class ViewPager2Adapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    companion object {
        val tabs: ArrayList</* res id */ Int> = arrayListOf(
            R.id.songs,
            R.id.albums,
            R.id.artists,
            R.id.genres,
            R.id.dates,
            R.id.folders,
            R.id.detailed_folders,
            R.id.playlists,
        )
    }

    fun getLabelResId(position: Int): Int =
        when (tabs[position]) {
            R.id.songs -> R.string.category_songs
            R.id.albums -> R.string.category_albums
            R.id.artists -> R.string.category_artists
            R.id.genres -> R.string.category_genres
            R.id.dates -> R.string.category_dates
            R.id.folders -> R.string.filesystem
            R.id.detailed_folders -> R.string.folders
            R.id.playlists -> R.string.category_playlists
            else -> throw IllegalArgumentException("Invalid position: $position")
        }

    override fun getItemCount(): Int = tabs.count()

    override fun createFragment(position: Int): Fragment =
        AdapterFragment().apply {
            arguments = Bundle().apply {
                putInt("ID", tabs[position])
            }
        }
}
