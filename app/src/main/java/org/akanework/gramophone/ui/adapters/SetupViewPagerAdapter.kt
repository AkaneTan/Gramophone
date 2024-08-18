package org.akanework.gramophone.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.ui.fragments.setup.IntroductionFragment
import org.akanework.gramophone.ui.fragments.setup.PermissionFragment

class SetupViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> IntroductionFragment()
            1 -> PermissionFragment()
            else -> throw IllegalArgumentException("Didn't find desired fragment!")
        }
}