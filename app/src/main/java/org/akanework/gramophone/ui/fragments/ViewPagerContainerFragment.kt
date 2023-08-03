package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

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
        val drawerLayout = rootView.findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = rootView.findViewById<NavigationView>(R.id.navigation_view)

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.shuffle -> {
                    true
                }
                else -> {
                    true
                }
            }
        }

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.songs -> {
                    viewPager2.setCurrentItem(0, true)
                    drawerLayout.close()
                    true
                }
                R.id.albums -> {
                    viewPager2.setCurrentItem(1, true)
                    drawerLayout.close()
                    true
                }
                R.id.artists -> {
                    viewPager2.setCurrentItem(2, true)
                    drawerLayout.close()
                    true
                }
                R.id.genres -> {
                    viewPager2.setCurrentItem(3, true)
                    drawerLayout.close()
                    true
                }
                R.id.dates -> {
                    viewPager2.setCurrentItem(4, true)
                    drawerLayout.close()
                    true
                }
                R.id.playlists -> {
                    viewPager2.setCurrentItem(5, true)
                    drawerLayout.close()
                    true
                }
                R.id.refresh -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        val pairObject = MediaStoreUtils.getAllSongs(requireContext())
                        withContext(Dispatchers.Main) {
                            libraryViewModel.mediaItemList.value = pairObject.songList
                            libraryViewModel.albumItemList.value = pairObject.albumList
                            libraryViewModel.artistItemList.value = pairObject.artistList
                            libraryViewModel.genreItemList.value = pairObject.genreList
                            libraryViewModel.dateItemList.value = pairObject.dateList
                            val snackBar = Snackbar.make(requireActivity().findViewById(R.id.fragment_container), "Refreshed ${libraryViewModel.mediaItemList.value!!.size} songs", Snackbar.LENGTH_LONG)
                            snackBar.setAction(R.string.dismiss) {
                                snackBar.dismiss()
                            }
                            snackBar.setBackgroundTint(MaterialColors.getColor(
                                snackBar.view,
                                com.google.android.material.R.attr.colorSurface
                            ))
                            snackBar.setActionTextColor(MaterialColors.getColor(
                                snackBar.view,
                                com.google.android.material.R.attr.colorPrimary
                            ))
                            snackBar.setTextColor(MaterialColors.getColor(
                                snackBar.view,
                                com.google.android.material.R.attr.colorOnSurface
                            ))
                            snackBar.show()
                        }
                    }
                    drawerLayout.close()
                    true
                }
                R.id.settings -> {
                    drawerLayout.close()
                    true
                }
                else -> throw IllegalStateException()
            }
        }

        // Handle click for navigationIcon.
        topAppBar.setNavigationOnClickListener {
            drawerLayout.open()
            when (viewPager2.currentItem) {
                0 -> {
                    navigationView.setCheckedItem(R.id.songs)
                }
                1 -> {
                    navigationView.setCheckedItem(R.id.albums)
                }
                2 -> {
                    navigationView.setCheckedItem(R.id.artists)
                }
                3 -> {
                    navigationView.setCheckedItem(R.id.genres)
                }
                4 -> {
                    navigationView.setCheckedItem(R.id.dates)
                }
                5 -> {
                    navigationView.setCheckedItem(R.id.playlists)
                }
                else -> throw IllegalStateException()
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