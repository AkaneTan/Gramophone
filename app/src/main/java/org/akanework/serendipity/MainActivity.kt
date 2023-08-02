package org.akanework.serendipity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.serendipity.logic.utils.MediaStoreUtils
import org.akanework.serendipity.ui.adapters.ViewPager2Adapter
import org.akanework.serendipity.ui.viewmodels.LibraryViewModel

class MainActivity : AppCompatActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            val pairObject = MediaStoreUtils.getAllSongs(applicationContext)
            withContext(Dispatchers.Main) {
                libraryViewModel.mediaItemList.value = pairObject.first
                libraryViewModel.albumItemList.value = pairObject.second
            }
        }

        // Set content Views.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Initialize layouts.
        val viewPager2 = findViewById<ViewPager2>(R.id.fragment_viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        // Connect ViewPager2.
        viewPager2.adapter = ViewPager2Adapter(this)
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
        viewPager2.offscreenPageLimit = 5
    }
}