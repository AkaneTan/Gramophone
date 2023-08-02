package org.akanework.serendipity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.akanework.serendipity.logic.utils.MediaStoreUtils
import org.akanework.serendipity.ui.adapters.MainViewPagerAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content Views.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Initialize layouts.
        val viewPager2 = findViewById<ViewPager2>(R.id.fragment_viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        // Connect ViewPager2.
        viewPager2.adapter = MainViewPagerAdapter(this)
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

        Log.d("TAGTAG", MediaStoreUtils.getAllSongs(this).size.toString())

    }
}