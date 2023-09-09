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
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel
import kotlin.random.Random

@UnstableApi
class ViewPagerFragment : Fragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    private lateinit var viewPager2: ViewPager2

    fun navigateViewPager(index: Int) {
        viewPager2.setCurrentItem(index, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    }

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
            (requireActivity() as MainActivity).navigateDrawer(viewPager2.currentItem)
        }

        // Connect ViewPager2.
        viewPager2.adapter = ViewPager2Adapter(childFragmentManager, lifecycle)
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text =
                when (position) {
                    0 -> getString(R.string.category_songs)
                    1 -> getString(R.string.category_albums)
                    2 -> getString(R.string.category_artists)
                    3 -> getString(R.string.category_genres)
                    4 -> getString(R.string.category_dates)
                    else -> "Unknown"
                }
        }.attach()

        return rootView
    }
}
