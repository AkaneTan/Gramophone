package org.akanework.gramophone

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.services.GramophonePlaybackService
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@UnstableApi class MainActivity : AppCompatActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var sessionToken: SessionToken
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private lateinit var bottomSheetPreviewCover: ImageView

    val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
                Glide.with(bottomSheetPreviewCover)
                    .load(controllerFuture.get().currentMediaItem?.mediaMetadata!!.artworkUri)
                    .placeholder(R.drawable.ic_default_cover)
                    .into(bottomSheetPreviewCover)
        }
    }

    override fun onStart() {
        sessionToken = SessionToken(this, ComponentName(this, GramophonePlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken)
            .buildAsync()
        controllerFuture.addListener(
            { controllerFuture.get().addListener(playerListener) },
            MoreExecutors.directExecutor()
        )
        Log.d("TAG", "onStart")
        super.onStart()
    }

    fun getSession() = sessionToken

    fun getPlayer() = controllerFuture.get()

    @SuppressLint("StringFormatMatches")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (libraryViewModel.mediaItemList.value!!.isEmpty()) {
            CoroutineScope(Dispatchers.Default).launch {
                val pairObject = MediaStoreUtils.getAllSongs(applicationContext)
                withContext(Dispatchers.Main) {
                    libraryViewModel.mediaItemList.value = pairObject.songList
                    libraryViewModel.albumItemList.value = pairObject.albumList
                    libraryViewModel.artistItemList.value = pairObject.artistList
                    libraryViewModel.genreItemList.value = pairObject.genreList
                    libraryViewModel.dateItemList.value = pairObject.dateList
                }
            }
        }

        // Set content Views.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Initialize layouts.
        val viewPager2 = findViewById<ViewPager2>(R.id.fragment_viewpager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        val standardBottomSheet = findViewById<FrameLayout>(R.id.player_layout)
        val standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet)

        bottomSheetPreviewCover = findViewById(R.id.album_cover)

        if (!this::controllerFuture.isInitialized || !controllerFuture.get().isPlaying) {
            standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

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
                        val pairObject = MediaStoreUtils.getAllSongs(applicationContext)
                        withContext(Dispatchers.Main) {
                            libraryViewModel.mediaItemList.value = pairObject.songList
                            libraryViewModel.albumItemList.value = pairObject.albumList
                            libraryViewModel.artistItemList.value = pairObject.artistList
                            libraryViewModel.genreItemList.value = pairObject.genreList
                            libraryViewModel.dateItemList.value = pairObject.dateList
                            val snackBar = Snackbar.make(viewPager2,
                                getString(
                                    R.string.refreshed_songs,
                                    libraryViewModel.mediaItemList.value!!.size
                                ), Snackbar.LENGTH_LONG)
                            snackBar.setAction(R.string.dismiss) {
                                snackBar.dismiss()
                            }
                            snackBar.setBackgroundTint(
                                MaterialColors.getColor(
                                snackBar.view,
                                com.google.android.material.R.attr.colorSurface
                            ))
                            snackBar.setActionTextColor(
                                MaterialColors.getColor(
                                snackBar.view,
                                com.google.android.material.R.attr.colorPrimary
                            ))
                            snackBar.setTextColor(
                                MaterialColors.getColor(
                                snackBar.view,
                                com.google.android.material.R.attr.colorOnSurface
                            ))
                            snackBar.anchorView = standardBottomSheet
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

    }
}