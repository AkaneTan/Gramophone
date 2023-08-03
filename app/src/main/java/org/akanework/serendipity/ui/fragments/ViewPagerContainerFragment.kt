package org.akanework.serendipity.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.serendipity.R
import org.akanework.serendipity.logic.utils.MediaStoreUtils
import org.akanework.serendipity.ui.adapters.ViewPager2Adapter
import org.akanework.serendipity.ui.viewmodels.LibraryViewModel

class ViewPagerContainerFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_viewpager_container, container, false)
        // Initialize layouts.
        val viewPager2 = rootView.findViewById<ViewPager2>(R.id.fragment_viewpager)
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tab_layout)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        Log.d("TAGTAG", "CREATED VIEW")

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    true
                }
                R.id.refresh -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val pairObject = MediaStoreUtils.getAllSongs(requireContext())
                        withContext(Dispatchers.Main) {
                            libraryViewModel.mediaItemList.value = pairObject.first
                            libraryViewModel.albumItemList.value = pairObject.second
                        }
                    }
                    true
                }
                else -> {
                    true
                }
            }
        }

        // Connect ViewPager2.
        viewPager2.adapter = ViewPager2Adapter(requireActivity())
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.category_songs)
                1 -> getString(R.string.category_albums)
                2 -> getString(R.string.category_artists)
                3 -> getString(R.string.category_genres)
                4 -> getString(R.string.category_dates)
                5 -> getString(R.string.category_playlists)
                else -> "Unknown"
            }
        }.attach()
        return rootView
    }
}