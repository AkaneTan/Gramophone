package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter

@androidx.annotation.OptIn(UnstableApi::class)
class ViewPagerFragment : BaseFragment(true) {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_viewpager, container, false)
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tab_layout)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

        val viewPager2 = rootView.findViewById<ViewPager2>(R.id.fragment_viewpager)

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.shuffle -> {
                    (requireActivity() as MainActivity).shuffle()
                }

                R.id.search -> {
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .addToBackStack("SEARCH")
                        .replace(R.id.container, SearchFragment())
                        .commit()
                }

                else -> throw IllegalStateException()
            }
            true
        }

        // Handle click for navigationIcon.
        topAppBar.setNavigationOnClickListener {
            (requireActivity() as MainActivity).navigateDrawer(viewPager2.currentItem)
        }

        // Connect ViewPager2.
        viewPager2.adapter = ViewPager2Adapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = getString(ViewPager2Adapter.getLabelResId(position))
        }.attach()

        return rootView
    }
}
