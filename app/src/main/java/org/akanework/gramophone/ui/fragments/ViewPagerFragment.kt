package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter
import org.akanework.gramophone.ui.components.MenuBottomSheet
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel
import kotlin.random.Random

@androidx.annotation.OptIn(UnstableApi::class)
class ViewPagerFragment : BaseFragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private lateinit var viewPager2: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_viewpager, container, false)
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tab_layout)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

        viewPager2 = rootView.findViewById(R.id.fragment_viewpager)

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.shuffle -> {
                    libraryViewModel.mediaItemList.value?.let { it1 ->
                        val controller = (requireActivity() as MainActivity).getPlayer()
                        controller.setMediaItems(it1)
                        controller.seekToDefaultPosition(Random.nextInt(0, it1.size))
                        controller.prepare()
                        controller.play()
                    }
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
            val menuBottomSheet = MenuBottomSheet()
            menuBottomSheet.show(requireActivity().supportFragmentManager, "MENU")
        }

        // Connect ViewPager2.
        viewPager2.adapter = ViewPager2Adapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = getString(ViewPager2Adapter.getLabelResId(position))
        }.attach()

        return rootView
    }
}
