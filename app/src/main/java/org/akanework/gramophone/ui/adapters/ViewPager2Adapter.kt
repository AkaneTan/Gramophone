package org.akanework.gramophone.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.AlbumFragment
import org.akanework.gramophone.ui.fragments.ArtistFragment
import org.akanework.gramophone.ui.fragments.DateFragment
import org.akanework.gramophone.ui.fragments.GenreFragment
import org.akanework.gramophone.ui.fragments.PlaylistFragment
import org.akanework.gramophone.ui.fragments.SongFragment

/**
 * This is the ViewPager2 adapter.
 */
class ViewPager2Adapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    companion object {
        val tabs: Map<Int, /* res id */ Int> = mapOf(
            Pair(0, R.id.songs),
            Pair(1, R.id.albums),
            Pair(2, R.id.artists),
            Pair(3, R.id.genres),
            Pair(4, R.id.dates),
            Pair(5, R.id.playlists)
        )

        fun getLabelResId(position: Int): Int =
            when (tabs.getValue(position)) {
                R.id.songs -> R.string.category_songs
                R.id.albums -> R.string.category_albums
                R.id.artists -> R.string.category_artists
                R.id.genres -> R.string.category_genres
                R.id.dates -> R.string.category_dates
                R.id.playlists -> R.string.category_playlists
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
    }

    override fun getItemCount(): Int = tabs.count()

    override fun createFragment(position: Int): Fragment =
        when (tabs.getValue(position)) {
            R.id.songs -> SongFragment()
            R.id.albums -> AlbumFragment()
            R.id.artists -> ArtistFragment()
            R.id.genres -> GenreFragment()
            R.id.dates -> DateFragment()
            R.id.playlists -> PlaylistFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
}
