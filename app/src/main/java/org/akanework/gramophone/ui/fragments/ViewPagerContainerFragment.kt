package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.setCurrentItemInterpolated
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.ViewPagerAdapter

class ViewPagerContainerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_viewpager_container, container, false)
        val viewPager: ViewPager2 = rootView.findViewById(R.id.viewPager2)
        val adapter = ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 9999

        (requireActivity() as MainActivity).connectBottomNavigationView { it ->
            when (it.itemId) {
                R.id.homepage -> viewPager.setCurrentItemInterpolated(0)
                R.id.browse -> viewPager.setCurrentItemInterpolated(1)
                R.id.library -> viewPager.setCurrentItemInterpolated(2)
                R.id.search -> viewPager.setCurrentItemInterpolated(3)
                else -> throw IllegalArgumentException("Illegal itemId: ${it.itemId}")
            }
            true
        }

        return rootView
    }
}