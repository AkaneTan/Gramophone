package org.akanework.gramophone.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.BrowseFragment
import org.akanework.gramophone.ui.fragments.HomepageFragment
import org.akanework.gramophone.ui.fragments.LibraryFragment
import org.akanework.gramophone.ui.fragments.SearchFragment

class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    companion object {
        val tabs: ArrayList<Int> = arrayListOf(
            R.id.homepage,
            R.id.browse,
            R.id.library,
            R.id.search
        )
    }

    override fun getItemCount(): Int = tabs.count()

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> HomepageFragment()
            1 -> BrowseFragment()
            2 -> LibraryFragment()
            3 -> SearchFragment()
            else -> throw IllegalArgumentException("Didn't find desired fragment!")
        }
}