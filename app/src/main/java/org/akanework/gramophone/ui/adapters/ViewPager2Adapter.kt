package org.akanework.gramophone.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.ui.fragments.AlbumFragment
import org.akanework.gramophone.ui.fragments.ArtistFragment
import org.akanework.gramophone.ui.fragments.DateFragment
import org.akanework.gramophone.ui.fragments.GenreFragment
import org.akanework.gramophone.ui.fragments.PlaylistFragment
import org.akanework.gramophone.ui.fragments.SongFragment

/**
 * This is the ViewPager2 adapter.
 */
class ViewPager2Adapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SongFragment()
        1 -> AlbumFragment()
        2 -> ArtistFragment()
        3 -> GenreFragment()
        4 -> DateFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}