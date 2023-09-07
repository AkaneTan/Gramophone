package org.akanework.gramophone

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentContainerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.services.GramophonePlaybackService
import org.akanework.gramophone.logic.utils.GramophoneUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.SettingsFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@UnstableApi class MainActivity : AppCompatActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var sessionToken: SessionToken
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private lateinit var bottomSheetPreviewCover: ImageView
    private lateinit var bottomSheetPreviewTitle: TextView
    private lateinit var bottomSheetPreviewSubtitle: TextView
    private lateinit var bottomSheetPreviewControllerButton: MaterialButton
    private lateinit var bottomSheetPreviewNextButton: MaterialButton
    private lateinit var bottomSheetFullCover: ImageView
    private lateinit var bottomSheetFullTitle: TextView
    private lateinit var bottomSheetFullSubtitle: TextView
    private lateinit var bottomSheetFullControllerButton: MaterialButton
    private lateinit var bottomSheetFullNextButton: MaterialButton
    private lateinit var bottomSheetFullPreviousButton: MaterialButton
    private lateinit var bottomSheetFullDuration: TextView
    private lateinit var bottomSheetFullPosition: TextView
    private lateinit var bottomSheetShuffleButton: MaterialButton
    private lateinit var bottomSheetLoopButton: MaterialButton
    private lateinit var bottomSheetPlaylistButton: MaterialButton
    private lateinit var bottomSheetLyricButton: MaterialButton
    private lateinit var bottomSheetTimerButton: MaterialButton
    private lateinit var bottomSheetFullSlider: Slider

    private lateinit var standardBottomSheet: FrameLayout
    private lateinit var standardBottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var previewPlayer: RelativeLayout
    private lateinit var navigationView: NavigationView

    private var isPlayerPlaying = false
    private var isUserTracking = false
    private var runnableRunning = 0

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            updateSongInfo(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            isPlayerPlaying = isPlaying
            val instance = controllerFuture.get()
            Log.d("TAG", "isPlaying, $isPlaying")
            if (isPlaying) {
                bottomSheetPreviewControllerButton.icon =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.pause_art)
                bottomSheetFullControllerButton.icon =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.pause_art)
            } else if (instance.playbackState != 2) {
                Log.d("TAG", "Triggered, ${instance.playbackState}")
                bottomSheetPreviewControllerButton.icon =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.play_art)
                bottomSheetFullControllerButton.icon =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.play_art)
            }
            if (isPlaying) {
                val positionRunnable = object : Runnable {
                    override fun run() {
                        val position = GramophoneUtils.convertDurationToTimeStamp(instance!!.currentPosition)
                        Log.d("TAG", position)
                        if (runnableRunning == 1) {
                            val duration = libraryViewModel.durationItemList.value?.get(
                                instance.currentMediaItem?.mediaId?.toLong())
                            if (duration != null && !isUserTracking) {
                                bottomSheetFullSlider.value = instance.currentPosition.toFloat() / duration.toFloat()
                                bottomSheetFullPosition.text = position
                            }
                        }
                        if (instance.isPlaying) {
                            Handler(Looper.getMainLooper()).postDelayed(this, 500)
                        } else {
                            runnableRunning --
                        }
                        Log.d("TAG", "Runnables: $runnableRunning")
                    }
                }
                if (runnableRunning == 0) {
                    Handler(Looper.getMainLooper()).postDelayed(positionRunnable, 500)
                    runnableRunning ++
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            Log.d("TAG", "PlaybackState: $playbackState")
        }
    }

    fun updateSongInfo(mediaItem: MediaItem?) {
        Log.d("TAG", "${!controllerFuture.get().isPlaying}")
        val instance = controllerFuture.get()
        if (instance.mediaItemCount != 0) {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    Log.d("TAG", "PlaybackState: ${instance.playbackState}, isPlaying: ${instance.isPlaying}")
                    if (instance.isPlaying) {
                        Log.d("TAG", "REACHED1")
                        bottomSheetPreviewControllerButton.icon =
                            AppCompatResources.getDrawable(applicationContext, R.drawable.pause_art)
                        bottomSheetFullControllerButton.icon =
                            AppCompatResources.getDrawable(applicationContext, R.drawable.pause_art)
                    } else if (instance.playbackState != 2) {
                        Log.d("TAG", "REACHED2")
                        bottomSheetPreviewControllerButton.icon =
                            AppCompatResources.getDrawable(applicationContext, R.drawable.play_art)
                        bottomSheetFullControllerButton.icon =
                            AppCompatResources.getDrawable(applicationContext, R.drawable.play_art)
                    }
                    if (standardBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                        standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        previewPlayer.alpha = 1f
                        Handler(Looper.getMainLooper()).postDelayed({
                            standardBottomSheetBehavior.isHideable = false
                        }, 200 )
                    }}, 200)
            Glide.with(bottomSheetPreviewCover)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .placeholder(R.drawable.ic_default_cover)
                .into(bottomSheetPreviewCover)
            Glide.with(bottomSheetFullCover)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .placeholder(R.drawable.ic_default_cover)
                .into(bottomSheetFullCover)
            bottomSheetPreviewTitle.text = mediaItem?.mediaMetadata?.title
            bottomSheetPreviewSubtitle.text = mediaItem?.mediaMetadata?.artist
            bottomSheetFullTitle.text = mediaItem?.mediaMetadata?.title
            bottomSheetFullSubtitle.text = mediaItem?.mediaMetadata?.artist
            bottomSheetFullDuration.text =
                mediaItem?.mediaId?.let { libraryViewModel.durationItemList.value?.get(it.toLong()) }
                    ?.let { GramophoneUtils.convertDurationToTimeStamp(it) }
        } else {
            if (!standardBottomSheetBehavior.isHideable) {
                standardBottomSheetBehavior.isHideable = true
            }
            Handler(Looper.getMainLooper()).postDelayed({
                standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }, 200)
        }
    }

    fun navigateDrawer(int: Int) {
        drawerLayout.open()
        when (int) {
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
            else -> throw IllegalStateException()
        }
    }

    override fun onStart() {
        sessionToken = SessionToken(this, ComponentName(this, GramophonePlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken)
            .buildAsync()
        controllerFuture.addListener(
            {   val controller = controllerFuture.get()
                controller.addListener(playerListener)
                bottomSheetPreviewControllerButton.setOnClickListener {
                    if (controller.isPlaying) {
                        controllerFuture.get().pause()
                    } else {
                        controllerFuture.get().play()
                    }
                }
                bottomSheetFullControllerButton.setOnClickListener {
                    if (controller.isPlaying) {
                        controllerFuture.get().pause()
                    } else {
                        controllerFuture.get().play()
                    }
                }
                bottomSheetPreviewNextButton.setOnClickListener {
                    controllerFuture.get().seekToNextMediaItem()
                }
                bottomSheetFullPreviousButton.setOnClickListener {
                    controllerFuture.get().seekToPreviousMediaItem()
                }
                bottomSheetFullNextButton.setOnClickListener {
                    controllerFuture.get().seekToNextMediaItem()
                }
                bottomSheetShuffleButton.isChecked = controller.shuffleModeEnabled
                bottomSheetShuffleButton.addOnCheckedChangeListener { _, isChecked ->
                    controllerFuture.get().shuffleModeEnabled = isChecked
                }
                when (controller.repeatMode) {
                    REPEAT_MODE_ALL -> {
                        bottomSheetLoopButton.isChecked = true
                        bottomSheetLoopButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
                    }
                    REPEAT_MODE_OFF -> {
                        bottomSheetLoopButton.isChecked = false
                        bottomSheetLoopButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
                    }
                    REPEAT_MODE_ONE -> {
                        bottomSheetLoopButton.isChecked = true
                        bottomSheetLoopButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one)
                    }
                }
                bottomSheetLoopButton.addOnCheckedChangeListener { _, _ ->
                    val instance = controllerFuture.get()
                    when (instance.repeatMode) {
                        REPEAT_MODE_ALL -> {
                            bottomSheetLoopButton.isChecked = true
                            bottomSheetLoopButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one)
                            instance.repeatMode = REPEAT_MODE_ONE
                        }
                        REPEAT_MODE_OFF -> {
                            bottomSheetLoopButton.isChecked = true
                            bottomSheetLoopButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
                            instance.repeatMode = REPEAT_MODE_ALL

                        }
                        REPEAT_MODE_ONE -> {
                            bottomSheetLoopButton.isChecked = false
                            bottomSheetLoopButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat)
                            instance.repeatMode = REPEAT_MODE_OFF
                        }
                    }
                }
                updateSongInfo(controller.currentMediaItem)
                if (controllerFuture.get().isPlaying) {
                    val positionRunnable = object : Runnable {
                        private val instance = controllerFuture.get()
                        override fun run() {
                            val position =
                                GramophoneUtils.convertDurationToTimeStamp(instance!!.currentPosition)
                            Log.d("TAG", position)
                            if (runnableRunning == 1) {
                                val duration = libraryViewModel.durationItemList.value?.get(
                                    instance.currentMediaItem?.mediaId?.toLong())
                                if (duration != null && !isUserTracking) {
                                    bottomSheetFullSlider.value = instance.currentPosition.toFloat() / duration.toFloat()
                                    bottomSheetFullPosition.text = position
                                }
                            }
                            if (instance.isPlaying) {
                                Handler(Looper.getMainLooper()).postDelayed(this, 500)
                            } else {
                                runnableRunning --
                            }
                            Log.d("TAG", "Runnables: $runnableRunning")
                        }
                    }
                    if (runnableRunning == 0) {
                        Handler(Looper.getMainLooper()).postDelayed(positionRunnable, 500)
                        runnableRunning ++
                    }
                }

                // When the slider is dragged by user, mark it
                // to use this state later.
                val touchListener: Slider.OnSliderTouchListener = object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        isUserTracking = true
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        // This value is multiplied by 1000 is because
                        // when the number is too big (like when toValue
                        // used the duration directly) we might encounter
                        // some performance problem.
                        val instance = controllerFuture.get()
                        val mediaId = instance.currentMediaItem?.mediaId
                        if (mediaId != null) {
                            instance.seekTo((slider.value * libraryViewModel.durationItemList.value!![mediaId.toLong()]!!).toLong())
                        }
                        isUserTracking = false
                    }
                }

                bottomSheetFullSlider.addOnChangeListener { _, value, isUser ->
                    if (isUser) {
                        val instance = controllerFuture.get()
                        val dest = instance.currentMediaItem?.mediaId?.let { libraryViewModel.durationItemList.value?.get(it.toLong()) }
                        if (dest != null) {
                            bottomSheetFullPosition.text = GramophoneUtils.convertDurationToTimeStamp((value * dest).toLong())
                        }
                    }
                }

                bottomSheetFullSlider.addOnSliderTouchListener(touchListener)
            },
            MoreExecutors.directExecutor()
        )

        Log.d("TAG", "onStart")
        super.onStart()
    }

    fun getPlayer(): MediaController = controllerFuture.get()

    private fun updateLibrary() {
        CoroutineScope(Dispatchers.Default).launch {
            updateLibraryWithInCoroutine()
        }
    }

    private suspend fun updateLibraryWithInCoroutine() {
        val pairObject = MediaStoreUtils.getAllSongs(applicationContext)
        withContext(Dispatchers.Main) {
            libraryViewModel.mediaItemList.value = pairObject.songList
            libraryViewModel.albumItemList.value = pairObject.albumList
            libraryViewModel.artistItemList.value = pairObject.artistList
            libraryViewModel.genreItemList.value = pairObject.genreList
            libraryViewModel.dateItemList.value = pairObject.dateList
            libraryViewModel.durationItemList.value = pairObject.durationList
            libraryViewModel.fileUriList.value = pairObject.fileUriList
            libraryViewModel.mimeTypeList.value = pairObject.mimeTypeList
        }
    }

    @SuppressLint("StringFormatMatches")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.postponeEnterTransition(this)

        if (libraryViewModel.mediaItemList.value!!.isEmpty()) {
            updateLibrary()
        }

        val params = window.attributes
        params.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = params

        // Set content Views.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Initialize layouts.
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        standardBottomSheet = findViewById(R.id.player_layout)
        standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet)

        bottomSheetPreviewCover = findViewById(R.id.preview_album_cover)
        bottomSheetPreviewTitle = findViewById(R.id.preview_song_name)
        bottomSheetPreviewSubtitle = findViewById(R.id.preview_artist_name)
        bottomSheetPreviewControllerButton = findViewById(R.id.preview_control)
        bottomSheetPreviewNextButton = findViewById(R.id.preview_next)

        bottomSheetFullCover = findViewById(R.id.full_sheet_cover)
        bottomSheetFullTitle = findViewById(R.id.full_song_name)
        bottomSheetFullSubtitle = findViewById(R.id.full_song_artist)
        bottomSheetFullPreviousButton = findViewById(R.id.sheet_previous_song)
        bottomSheetFullControllerButton = findViewById(R.id.sheet_mid_button)
        bottomSheetFullNextButton = findViewById(R.id.sheet_next_song)
        bottomSheetFullPosition = findViewById(R.id.position)
        bottomSheetFullDuration = findViewById(R.id.duration)
        bottomSheetFullSlider = findViewById(R.id.slider)
        bottomSheetShuffleButton = findViewById(R.id.sheet_random)
        bottomSheetLoopButton = findViewById(R.id.sheet_loop)
        bottomSheetLyricButton = findViewById(R.id.lyrics)
        bottomSheetTimerButton = findViewById(R.id.timer)
        bottomSheetPlaylistButton = findViewById(R.id.playlist)

        standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val fragmentContainerView: FragmentContainerView = findViewById(R.id.container)

        navigationView.setNavigationItemSelectedListener {
            val viewPager2 = fragmentContainerView.findViewById<ViewPager2>(R.id.fragment_viewpager)
            when (it.itemId) {
                R.id.songs -> {
                    viewPager2.setCurrentItem(0, true)
                    drawerLayout.close()
                }
                R.id.albums -> {
                    viewPager2.setCurrentItem(1, true)
                    drawerLayout.close()
                }
                R.id.artists -> {
                    viewPager2.setCurrentItem(2, true)
                    drawerLayout.close()
                }
                R.id.genres -> {
                    viewPager2.setCurrentItem(3, true)
                    drawerLayout.close()
                }
                R.id.dates -> {
                    viewPager2.setCurrentItem(4, true)
                    drawerLayout.close()
                }
                R.id.refresh -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        updateLibraryWithInCoroutine()
                        withContext(Dispatchers.Main) {
                            val snackBar = Snackbar.make(fragmentContainerView,
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
                }
                R.id.settings -> {
                    supportFragmentManager.beginTransaction()
                        .addToBackStack("SETTINGS")
                        .replace(R.id.container, SettingsFragment())
                        .commit()
                    Handler(Looper.getMainLooper()).postDelayed({
                        drawerLayout.close()
                    }, 50)
                }
                else -> throw IllegalStateException()
            }
            true
        }

        previewPlayer = findViewById(R.id.preview_player)
        val fullPlayer = findViewById<RelativeLayout>(R.id.full_player)

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED && previewPlayer.isVisible) {
                    fullPlayer.visibility = GONE
                    previewPlayer.alpha = 1f
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    fullPlayer.visibility = VISIBLE
                    previewPlayer.visibility = VISIBLE
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    previewPlayer.visibility = GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                previewPlayer.alpha = 1 - (slideOffset)
                fullPlayer.alpha = slideOffset
            }
        }

        standardBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask if was denied.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                    Constants.PERMISSION_READ_MEDIA_AUDIO
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask if was denied.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.PERMISSION_READ_EXTERNAL_STORAGE
                )
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.PERMISSION_READ_MEDIA_AUDIO -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }
            Constants.PERMISSION_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        controllerFuture.get().removeListener(playerListener)
        controllerFuture.get().release()
    }
}