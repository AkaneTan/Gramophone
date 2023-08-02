package org.akanework.serendipity.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.serendipity.ui.fragments.AlbumFragment
import org.akanework.serendipity.ui.fragments.ArtistFragment
import org.akanework.serendipity.ui.fragments.PlaylistFragment
import org.akanework.serendipity.ui.fragments.SongFragment

/**
 * This is the ViewPager2 adapter.
 */
class MainViewPagerAdapter(fragmentManager: FragmentActivity) :
    FragmentStateAdapter(fragmentManager) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SongFragment()
        1 -> AlbumFragment()
        2 -> ArtistFragment()
        3 -> PlaylistFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}