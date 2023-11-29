/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.components

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.GramophonePlaybackService
import org.akanework.gramophone.logic.dp
import org.akanework.gramophone.logic.fadInAnimation
import org.akanework.gramophone.logic.fadOutAnimation
import org.akanework.gramophone.logic.getTimer
import org.akanework.gramophone.logic.getUri
import org.akanework.gramophone.logic.hasTimer
import org.akanework.gramophone.logic.playOrPause
import org.akanework.gramophone.logic.px
import org.akanework.gramophone.logic.setTextAnimation
import org.akanework.gramophone.logic.setTimer
import org.akanework.gramophone.logic.startAnimation
import org.akanework.gramophone.logic.ui.MyBottomSheetBehavior
import org.akanework.gramophone.logic.utils.CalculationUtils
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.measureTimeMillis


class PlayerBottomSheet private constructor(
    context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes),
    Player.Listener, DefaultLifecycleObserver {
    constructor(context: Context, attributeSet: AttributeSet?)
            : this(context, attributeSet, 0, 0)

    companion object {
        const val BACKGROUND_COLOR_TRANSITION_SEC: Long = 300
        const val FOREGROUND_COLOR_TRANSITION_SEC: Long = 150
        const val LYRIC_FADE_TRANSITION_SEC: Long = 125
    }

    private var sessionToken: SessionToken? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val touchListener: SeekBar.OnSeekBarChangeListener
    private val bottomSheetPreviewCover: ImageView
    private val bottomSheetPreviewTitle: TextView
    private val bottomSheetPreviewSubtitle: TextView
    private val bottomSheetPreviewControllerButton: MaterialButton
    private val bottomSheetPreviewNextButton: MaterialButton
    private val bottomSheetFullCover: ImageView
    private val bottomSheetFullTitle: TextView
    private val bottomSheetFullSubtitle: TextView
    private val bottomSheetFullControllerButton: MaterialButton
    private val bottomSheetFullNextButton: MaterialButton
    private val bottomSheetFullPreviousButton: MaterialButton
    private val bottomSheetFullDuration: TextView
    private val bottomSheetFullPosition: TextView
    private val bottomSheetFullSlideUpButton: MaterialButton
    private val bottomSheetShuffleButton: MaterialButton
    private val bottomSheetLoopButton: MaterialButton
    private val bottomSheetPlaylistButton: MaterialButton
    private val bottomSheetTimerButton: MaterialButton
    private val bottomSheetFavoriteButton: MaterialButton
    private val bottomSheetLyricButton: MaterialButton
    private val bottomSheetFullSeekBar: SeekBar
    private val bottomSheetFullSlider: Slider
    private val bottomSheetFullCoverFrame: MaterialCardView
    private val bottomSheetFullLyricRecyclerView: RecyclerView
    private val bottomSheetFullLyricList: MutableList<MediaStoreUtils.Lyric> = mutableListOf()
    private val bottomSheetFullLyricAdapter: LyricAdapter = LyricAdapter(bottomSheetFullLyricList, activity)
    private val bottomSheetFullLyricLinearLayoutManager = LinearLayoutManager(context)
    private val bottomSheetFullLyricGradientViewUp: View
    private val bottomSheetFullLyricGradientViewDown: View
    private var standardBottomSheetBehavior: MyBottomSheetBehavior<FrameLayout>? = null
    private var bottomSheetBackCallback: OnBackPressedCallback? = null
    private val fullPlayer: View
    private val previewPlayer: View
    private val progressDrawable: SquigglyProgress
    private var isLegacyProgressEnabled: Boolean = false
    private var fullPlayerFinalColor: Int = -1
    private var colorPrimaryFinalColor: Int = -1
    private var colorSecondaryContainerFinalColor: Int = -1
    private var colorOnSecondaryContainerFinalColor: Int = -1

    private var playlistNowPlaying: TextView? = null
    private var playlistNowPlayingCover: ImageView? = null

    private var wrappedContext: Context? = null
    private var currentJob: Job? = null

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val activity
        get() = context as MainActivity
    private val lifecycleOwner: LifecycleOwner
        get() = activity
    private val handler = Handler(Looper.getMainLooper())
    private val instance: MediaController
        get() = controllerFuture!!.get()
    private var isUserTracking = false
    private var runnableRunning = false
    private var ready = false
        set(value) {
            field = value
            if (value) onUiReadyListener?.run()
        }
    private/*public when needed*/ var waitedForContainer = true
    private/*public when needed*/ var onUiReadyListener: Runnable? = null
        set(value) {
            field = value
            if (ready) onUiReadyListener?.run()
        }
    var visible = false
        set(value) {
            if (field != value) {
                field = value
                standardBottomSheetBehavior?.state =
                    if (controllerFuture?.isDone == true
                        && controllerFuture!!.get().mediaItemCount != 0 && value
                    ) {
                        if (standardBottomSheetBehavior?.state
                            != BottomSheetBehavior.STATE_EXPANDED
                        )
                            BottomSheetBehavior.STATE_COLLAPSED
                        else BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        BottomSheetBehavior.STATE_HIDDEN
                    }
            }
        }
    val actuallyVisible: Boolean
        get() = standardBottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN

    init {
        isLegacyProgressEnabled = prefs.getBoolean("default_progress_bar", false)
        inflate(context, R.layout.bottom_sheet_impl, this)
        id = R.id.player_layout
        bottomSheetPreviewCover = findViewById(R.id.preview_album_cover)
        bottomSheetPreviewTitle = findViewById(R.id.preview_song_name)
        bottomSheetPreviewSubtitle = findViewById(R.id.preview_artist_name)
        bottomSheetPreviewControllerButton = findViewById(R.id.preview_control)
        bottomSheetPreviewNextButton = findViewById(R.id.preview_next)
        bottomSheetFullCover = findViewById(R.id.full_sheet_cover)
        bottomSheetFullCoverFrame = findViewById(R.id.cover_frame)
        bottomSheetFullTitle = findViewById(R.id.full_song_name)
        bottomSheetFullSubtitle = findViewById(R.id.full_song_artist)
        bottomSheetFullPreviousButton = findViewById(R.id.sheet_previous_song)
        bottomSheetFullControllerButton = findViewById(R.id.sheet_mid_button)
        bottomSheetFullNextButton = findViewById(R.id.sheet_next_song)
        bottomSheetFullPosition = findViewById(R.id.position)
        bottomSheetFullDuration = findViewById(R.id.duration)
        bottomSheetFullSeekBar = findViewById(R.id.slider_squiggly)
        bottomSheetFullSlider = findViewById(R.id.slider_vert)
        bottomSheetFullSlideUpButton = findViewById(R.id.slide_down)
        bottomSheetShuffleButton = findViewById(R.id.sheet_random)
        bottomSheetLoopButton = findViewById(R.id.sheet_loop)
        bottomSheetTimerButton = findViewById(R.id.timer)
        bottomSheetFavoriteButton = findViewById(R.id.favor)
        bottomSheetPlaylistButton = findViewById(R.id.playlist)
        bottomSheetLyricButton = findViewById(R.id.lyrics)
        bottomSheetFullLyricRecyclerView = findViewById(R.id.lyric_frame)
        bottomSheetFullLyricGradientViewUp = findViewById(R.id.gradient_view_up)
        bottomSheetFullLyricGradientViewDown = findViewById(R.id.gradient_view_down)
        previewPlayer = findViewById(R.id.preview_player)
        fullPlayer = findViewById(R.id.full_player)
        fullPlayerFinalColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurface
        )
        colorPrimaryFinalColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorPrimary
        )
        colorOnSecondaryContainerFinalColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnSecondaryContainer
        )
        colorSecondaryContainerFinalColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSecondaryContainer
        )
        if (isLegacyProgressEnabled) {
            bottomSheetFullSlider.visibility = View.VISIBLE
            bottomSheetFullSeekBar.visibility = View.GONE
        } else {
            bottomSheetFullSlider.visibility = View.GONE
            bottomSheetFullSeekBar.visibility = View.VISIBLE
        }
        ViewCompat.setOnApplyWindowInsetsListener(previewPlayer) { view, insets ->
            view.onApplyWindowInsets(insets.toWindowInsets())
            doOnLayout {
                val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                previewPlayer.setPadding(0, 0, 0, navBarInset.bottom)
                fullPlayer.setPadding(0, statusBarInset.top, 0, navBarInset.bottom)
                previewPlayer.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.UNSPECIFIED
                )
                standardBottomSheetBehavior?.setPeekHeight(previewPlayer.measuredHeight, false)
            }
            return@setOnApplyWindowInsetsListener insets
        }

        val seekBarProgressWavelength =
            getContext().resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_wavelength)
                .toFloat()
        val seekBarProgressAmplitude =
            getContext().resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_amplitude)
                .toFloat()
        val seekBarProgressPhase =
            getContext().resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_phase)
                .toFloat()
        val seekBarProgressStrokeWidth =
            getContext().resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_stroke_width)
                .toFloat()

        progressDrawable = SquigglyProgress()
        bottomSheetFullSeekBar.progressDrawable = progressDrawable
        progressDrawable.let {
            it.waveLength = seekBarProgressWavelength
            it.lineAmplitude = seekBarProgressAmplitude
            it.phaseSpeed = seekBarProgressPhase
            it.strokeWidth = seekBarProgressStrokeWidth
            it.transitionEnabled = true
            it.animate = false
            it.setTint(
                MaterialColors.getColor(
                    bottomSheetFullSeekBar,
                    com.google.android.material.R.attr.colorPrimary,
                )
            )
        }

        touchListener = object : SeekBar.OnSeekBarChangeListener, Slider.OnSliderTouchListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val dest = instance.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
                    if (dest != null) {
                        bottomSheetFullPosition.text =
                            CalculationUtils.convertDurationToTimeStamp((progress.toLong()))
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserTracking = true
                progressDrawable.animate = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // This value is multiplied by 1000 is because
                // when the number is too big (like when toValue
                // used the duration directly) we might encounter
                // some performance problem.
                val mediaId = instance.currentMediaItem?.mediaId
                if (mediaId != null) {
                    if (seekBar != null) {
                        instance.seekTo((seekBar.progress.toLong()))
                        updateLyric(seekBar.progress.toLong())
                    }
                }
                isUserTracking = false
                progressDrawable.animate = instance.isPlaying || instance.playWhenReady
            }

            override fun onStartTrackingTouch(slider: Slider) {
                isUserTracking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // This value is multiplied by 1000 is because
                // when the number is too big (like when toValue
                // used the duration directly) we might encounter
                // some performance problem.
                val mediaId = instance.currentMediaItem?.mediaId
                if (mediaId != null) {
                    instance.seekTo((slider.value.toLong()))
                    updateLyric(slider.value.toLong())
                }
                isUserTracking = false
            }
        }

        setOnClickListener {
            if (standardBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetTimerButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            val picker =
                MaterialTimePicker
                    .Builder()
                    .setHour(instance.getTimer() / 3600 / 1000)
                    .setMinute((instance.getTimer() % (3600 * 1000)) / (60 * 1000))
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                    .build()
            picker.addOnPositiveButtonClickListener {
                val destinationTime: Int = picker.hour * 1000 * 3600 + picker.minute * 1000 * 60
                instance.setTimer(destinationTime)
            }
            picker.addOnDismissListener {
                bottomSheetTimerButton.isChecked = instance.hasTimer()
            }
            picker.show(activity.supportFragmentManager, "timer")
        }

        bottomSheetLoopButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            instance.repeatMode = when (instance.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> throw IllegalStateException()
            }
        }

        bottomSheetFavoriteButton.addOnCheckedChangeListener { _, isChecked ->
            /*
            if (isChecked) {
                instance.currentMediaItem?.let { insertIntoPlaylist(it) }
            } else {
                instance.currentMediaItem?.let { removeFromPlaylist(it) }
            }
             */
        }

        bottomSheetPlaylistButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            val playlistBottomSheet = BottomSheetDialog(context)
            playlistBottomSheet.setContentView(R.layout.playlist_bottom_sheet)
            val recyclerView = playlistBottomSheet.findViewById<RecyclerView>(R.id.recyclerview)!!
            val playlistAdapter = PlaylistCardAdapter(dumpPlaylist(), activity)
            playlistNowPlaying = playlistBottomSheet.findViewById(R.id.now_playing)
            playlistNowPlaying!!.text = instance.currentMediaItem?.mediaMetadata?.title
            playlistNowPlayingCover = playlistBottomSheet.findViewById(R.id.now_playing_cover)
            Glide
                .with(playlistNowPlayingCover!!)
                .load(instance.currentMediaItem?.mediaMetadata?.artworkUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_default_cover)
                .into(playlistNowPlayingCover!!)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = playlistAdapter
            recyclerView.scrollToPosition(instance.currentMediaItemIndex)
            FastScrollerBuilder(recyclerView).useMd2Style().setTrackDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_transparent)!!).build()
            playlistBottomSheet.show()
        }

        bottomSheetPreviewControllerButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            instance.playOrPause()
        }
        bottomSheetFullControllerButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            instance.playOrPause()
        }
        bottomSheetPreviewNextButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            instance.seekToNextMediaItem()
        }
        bottomSheetFullPreviousButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            instance.seekToPreviousMediaItem()
        }
        bottomSheetFullNextButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            instance.seekToNextMediaItem()
        }
        bottomSheetShuffleButton.addOnCheckedChangeListener { _, isChecked ->
            instance.shuffleModeEnabled = isChecked
        }

        bottomSheetFullSlider.addOnChangeListener { _, value, isUser ->
            if (isUser) {
                val dest = instance.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
                if (dest != null) {
                    bottomSheetFullPosition.text =
                        CalculationUtils.convertDurationToTimeStamp((value).toLong())
                }
            }
        }

        bottomSheetFullSeekBar.setOnSeekBarChangeListener(touchListener)
        bottomSheetFullSlider.addOnSliderTouchListener(touchListener)

        bottomSheetFullSlideUpButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bottomSheetLyricButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            bottomSheetFullLyricRecyclerView.fadInAnimation(LYRIC_FADE_TRANSITION_SEC)
            bottomSheetFullLyricGradientViewUp.fadInAnimation(LYRIC_FADE_TRANSITION_SEC)
            bottomSheetFullLyricGradientViewDown.fadInAnimation(LYRIC_FADE_TRANSITION_SEC)
        }

        bottomSheetShuffleButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        }

        bottomSheetFullLyricRecyclerView.layoutManager =
            bottomSheetFullLyricLinearLayoutManager
        bottomSheetFullLyricRecyclerView.adapter =
            bottomSheetFullLyricAdapter

        removeColorScheme()
    }

    private val bottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(
            bottomSheet: View,
            newState: Int,
        ) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    fullPlayer.visibility = View.GONE
                    previewPlayer.visibility = View.VISIBLE
                    previewPlayer.alpha = 1f
                    fullPlayer.alpha = 0f
                    bottomSheetBackCallback!!.isEnabled = false
                }

                BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                    fullPlayer.visibility = View.VISIBLE
                    previewPlayer.visibility = View.VISIBLE
                }

                BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    previewPlayer.visibility = View.GONE
                    fullPlayer.visibility = View.VISIBLE
                    previewPlayer.alpha = 0f
                    fullPlayer.alpha = 1f
                    bottomSheetBackCallback!!.isEnabled = true
                }

                BottomSheetBehavior.STATE_HIDDEN -> {
                    previewPlayer.visibility = View.GONE
                    fullPlayer.visibility = View.GONE
                    previewPlayer.alpha = 0f
                    fullPlayer.alpha = 0f
                    bottomSheetBackCallback!!.isEnabled = false
                }
            }
        }

        override fun onSlide(
            bottomSheet: View,
            slideOffset: Float,
        ) {
            if (slideOffset < 0) {
                // hidden state
                previewPlayer.alpha = 1 - (-1 * slideOffset)
                fullPlayer.alpha = 0f
                return
            }
            previewPlayer.alpha = 1 - (slideOffset)
            fullPlayer.alpha = slideOffset
        }
    }

    fun updateLyric(duration: Long?) {
        if (bottomSheetFullLyricList.isNotEmpty()) {
            val newIndex: Int

            val filteredList = bottomSheetFullLyricList.filterIndexed { _, lyric ->
                lyric.timeStamp <= instance.currentPosition
            }

            newIndex = if (filteredList.isNotEmpty()) {
                filteredList.indices.maxBy {
                    filteredList[it].timeStamp
                }
            } else {
                -1
            }

            if (newIndex != -1 &&
                duration != null &&
                newIndex != bottomSheetFullLyricAdapter.currentBoldPos) {
                val smoothScroller = createSmoothScroller()
                smoothScroller.targetPosition = newIndex
                bottomSheetFullLyricLinearLayoutManager.startSmoothScroll(
                    smoothScroller
                )
                bottomSheetFullLyricAdapter.updateHighlight(newIndex)
            }
        }
    }

    private fun createSmoothScroller() =
        object : LinearSmoothScroller(context) {
            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                return super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference) + (context.resources.displayMetrics.heightPixels / 3).dp
            }

            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 150f / displayMetrics.densityDpi
            }
        }

    private val positionRunnable = object : Runnable {
        override fun run() {
            val position =
                CalculationUtils.convertDurationToTimeStamp(instance.currentPosition)
            if (runnableRunning) {
                val duration = instance.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
                if (duration != null && !isUserTracking) {
                    bottomSheetFullSeekBar.max = duration.toInt()
                    bottomSheetFullSeekBar.progress = instance.currentPosition.toInt()
                    bottomSheetFullSlider.valueTo = duration.toFloat()
                    bottomSheetFullSlider.value = instance.currentPosition.toFloat()
                    bottomSheetFullPosition.text = position
                }
                updateLyric(duration)
            }
            if (instance.isPlaying) {
                handler.postDelayed(this, instance.currentPosition % 1000)
            } else {
                runnableRunning = false
            }
        }
    }

    private fun resetToDefaultLyricPosition() {
        val smoothScroller = createSmoothScroller()
        smoothScroller.targetPosition = 0
        bottomSheetFullLyricLinearLayoutManager.startSmoothScroll(
            smoothScroller
        )
        bottomSheetFullLyricAdapter.updateHighlight(-1)
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        post {
            standardBottomSheetBehavior = MyBottomSheetBehavior.from(this)
            standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    if (bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE) {
                        bottomSheetFullLyricRecyclerView.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewUp.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewDown.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetLyricButton.isChecked = false
                    } else {
                        standardBottomSheetBehavior!!.startBackProgress(backEvent)
                    }
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    if (bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE) {
                        bottomSheetFullLyricRecyclerView.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewUp.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewDown.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetLyricButton.isChecked = false
                    } else {
                        standardBottomSheetBehavior!!.updateBackProgress(backEvent)
                    }
                }

                override fun handleOnBackPressed() {
                    if (bottomSheetFullLyricRecyclerView.visibility ==
                            View.VISIBLE) {
                        bottomSheetFullLyricRecyclerView.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewUp.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewDown.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetLyricButton.isChecked = false
                    } else {
                    standardBottomSheetBehavior!!.handleBackInvoked()
                    }
                }

                override fun handleOnBackCancelled() {
                    if (bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE) {
                        bottomSheetFullLyricRecyclerView.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewUp.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetFullLyricGradientViewDown.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                        bottomSheetLyricButton.isChecked = false
                    } else {
                        standardBottomSheetBehavior!!.cancelBackProgress()
                    }
                }
            }
            /*
            lyricSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackPressed() {
                    bottomSheetFullLyricRecyclerView.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                    bottomSheetFullLyricGradientViewUp.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                    bottomSheetFullLyricGradientViewDown.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                    bottomSheetLyricButton.isChecked = false
                    activity.onBackPressedDispatcher.addCallback(activity, bottomSheetBackCallback!!)
                    bottomSheetBackCallback!!.isEnabled = true
                }
            }

             */
            activity.onBackPressedDispatcher.addCallback(activity, bottomSheetBackCallback!!)
            standardBottomSheetBehavior!!.addBottomSheetCallback(bottomSheetCallback)
            lifecycleOwner.lifecycle.addObserver(this)
            doOnLayout {
                previewPlayer.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.UNSPECIFIED
                )
                standardBottomSheetBehavior?.setPeekHeight(previewPlayer.measuredHeight, false)
            }
        }
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        lifecycleOwner.lifecycle.removeObserver(this)
        standardBottomSheetBehavior!!.removeBottomSheetCallback(bottomSheetCallback)
        bottomSheetBackCallback!!.remove()
        standardBottomSheetBehavior = null
        onStop(lifecycleOwner)
    }

    fun getPlayer(): MediaController = instance

    fun removeColorScheme(removeWrappedContext: Boolean = true) {
        if (removeWrappedContext) {
            wrappedContext = null
        }
        CoroutineScope(Dispatchers.Default).launch {
            val colorSurface = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurface,
                -1
            )

            val colorOnSurface = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                -1
            )

            val colorOnSurfaceVariant = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                -1
            )

            val colorPrimary =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorPrimary,
                    -1
                )

            val colorSecondaryContainer =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    -1
                )

            val colorOnSecondaryContainer =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSecondaryContainer,
                    -1
                )

            val colorSurfaceContainerHighest =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorSurfaceContainerHighest,
                    -1
                )

            val selectorBackground =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.sl_check_button
                )

            val selectorFavBackground =
                AppCompatResources.getColorStateList(
                    context,
                    R.color.sl_fav_button
                )

            val colorAccent =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorAccent,
                    -1
                )

            val backgroundProcessedColor = ColorUtils.getColor(colorSurface, ColorUtils.ColorType.COLOR_BACKGROUND_ELEVATED, context)

            val surfaceTransition = ValueAnimator.ofArgb(
                fullPlayerFinalColor,
                backgroundProcessedColor
            )

            val primaryTransition = ValueAnimator.ofArgb(
                colorPrimaryFinalColor,
                colorPrimary
            )

            val secondaryContainerTransition = ValueAnimator.ofArgb(
                colorSecondaryContainerFinalColor,
                colorSecondaryContainer
            )

            val onSecondaryContainerTransition = ValueAnimator.ofArgb(
                colorOnSecondaryContainerFinalColor,
                colorOnSecondaryContainer
            )

            surfaceTransition.apply {
                addUpdateListener { animation ->
                    fullPlayer.setBackgroundColor(
                        animation.animatedValue as Int
                    )
                    bottomSheetFullLyricRecyclerView.setBackgroundColor(
                        animation.animatedValue as Int
                    )
                }
                duration = BACKGROUND_COLOR_TRANSITION_SEC
            }

            primaryTransition.apply {
                addUpdateListener { animation ->
                    val progressColor = animation.animatedValue as Int
                    bottomSheetFullSlider.thumbTintList =
                        ColorStateList.valueOf(progressColor)
                    bottomSheetFullSlider.trackActiveTintList =
                        ColorStateList.valueOf(progressColor)
                    bottomSheetFullSeekBar.progressTintList =
                        ColorStateList.valueOf(progressColor)
                    bottomSheetFullSeekBar.thumbTintList =
                        ColorStateList.valueOf(progressColor)
                }
                duration = BACKGROUND_COLOR_TRANSITION_SEC
            }

            secondaryContainerTransition.apply {
                addUpdateListener { animation ->
                    val progressColor = animation.animatedValue as Int
                    bottomSheetFullSeekBar.secondaryProgressTintList =
                        ColorStateList.valueOf(progressColor)
                    bottomSheetFullControllerButton.backgroundTintList =
                        ColorStateList.valueOf(progressColor)
                }
                duration = BACKGROUND_COLOR_TRANSITION_SEC
            }

            onSecondaryContainerTransition.apply {
                addUpdateListener { animation ->
                    val progressColor = animation.animatedValue as Int
                    bottomSheetFullControllerButton.iconTint =
                        ColorStateList.valueOf(progressColor)
                }
                duration = BACKGROUND_COLOR_TRANSITION_SEC
            }

            withContext(Dispatchers.Main) {
                surfaceTransition.start()
                primaryTransition.start()
                secondaryContainerTransition.start()
                onSecondaryContainerTransition.start()
            }

            delay(FOREGROUND_COLOR_TRANSITION_SEC)
            fullPlayerFinalColor = backgroundProcessedColor
            colorPrimaryFinalColor = colorPrimary
            colorSecondaryContainerFinalColor = colorSecondaryContainer
            colorOnSecondaryContainerFinalColor = colorOnSecondaryContainer

            withContext(Dispatchers.Main) {
                bottomSheetFullTitle.setTextColor(
                    colorOnSurface
                )
                bottomSheetFullSubtitle.setTextColor(
                    colorOnSurfaceVariant
                )
                bottomSheetFullCoverFrame.setCardBackgroundColor(
                    colorSurface
                )
                bottomSheetFullLyricAdapter.updateTextColor(
                    ColorUtils.getColor(
                        colorSecondaryContainerFinalColor,
                        ColorUtils.ColorType.COLOR_CONTRAST_FAINTED,
                        context
                    ),
                    colorPrimary
                )

                bottomSheetFullSlider.trackInactiveTintList =
                    ColorStateList.valueOf(colorSurfaceContainerHighest)

                bottomSheetTimerButton.iconTint =
                    selectorBackground
                bottomSheetPlaylistButton.iconTint =
                    selectorBackground
                bottomSheetShuffleButton.iconTint =
                    selectorBackground
                bottomSheetLoopButton.iconTint =
                    selectorBackground
                bottomSheetLyricButton.iconTint =
                    selectorBackground
                bottomSheetFavoriteButton.iconTint =
                    selectorFavBackground

                bottomSheetFullNextButton.iconTint =
                    ColorStateList.valueOf(colorOnSurface)
                bottomSheetFullPreviousButton.iconTint =
                    ColorStateList.valueOf(colorOnSurface)
                bottomSheetFullSlideUpButton.iconTint =
                    ColorStateList.valueOf(colorOnSurface)

                bottomSheetFullPosition.setTextColor(
                    colorAccent
                )
                bottomSheetFullDuration.setTextColor(
                    colorAccent
                )

                val gradientDrawableUp = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(backgroundProcessedColor, Color.TRANSPARENT)
                )
                gradientDrawableUp.gradientType = GradientDrawable.LINEAR_GRADIENT
                val gradientDrawableDown = GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    intArrayOf(backgroundProcessedColor, Color.TRANSPARENT)
                )
                gradientDrawableDown.gradientType = GradientDrawable.LINEAR_GRADIENT
                bottomSheetFullLyricGradientViewUp.background = gradientDrawableUp
                bottomSheetFullLyricGradientViewDown.background = gradientDrawableDown
            }
        }
    }

    @Suppress("DEPRECATION")
    fun addColorScheme() {
        val mediaItem = instance.currentMediaItem
        currentJob?.cancel()
        currentJob = CoroutineScope(Dispatchers.Default).launch {

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    activity.contentResolver,
                    mediaItem?.mediaMetadata?.artworkUri
                )
                val originalBitmap: Bitmap = bitmap
                val targetWidth = bitmap.width / 16
                val targetHeight = bitmap.height / 16
                val scaledBitmap =
                    Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

                wrappedContext = DynamicColors.wrapContextIfAvailable(
                    context,
                    DynamicColorsOptions.Builder()
                        .setContentBasedSource(scaledBitmap)
                        .build()
                )

                val colorSurface = MaterialColors.getColor(
                    wrappedContext!!,
                    com.google.android.material.R.attr.colorSurface,
                    -1
                )

                val colorOnSurface = MaterialColors.getColor(
                    wrappedContext!!,
                    com.google.android.material.R.attr.colorOnSurface,
                    -1
                )

                val colorOnSurfaceVariant = MaterialColors.getColor(
                    wrappedContext!!,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    -1
                )

                val colorPrimary =
                    MaterialColors.getColor(
                        wrappedContext!!,
                        com.google.android.material.R.attr.colorPrimary,
                        -1
                    )

                val colorSecondaryContainer =
                    MaterialColors.getColor(
                        wrappedContext!!,
                        com.google.android.material.R.attr.colorSecondaryContainer,
                        -1
                    )

                val colorOnSecondaryContainer =
                    MaterialColors.getColor(
                        wrappedContext!!,
                        com.google.android.material.R.attr.colorOnSecondaryContainer,
                        -1
                    )

                val colorSurfaceContainerHighest =
                    MaterialColors.getColor(
                        wrappedContext!!,
                        com.google.android.material.R.attr.colorSurfaceContainerHighest,
                        -1
                    )

                val selectorBackground =
                    AppCompatResources.getColorStateList(
                        wrappedContext!!,
                        R.color.sl_check_button
                    )

                val selectorFavBackground =
                    AppCompatResources.getColorStateList(
                        wrappedContext!!,
                        R.color.sl_fav_button
                    )

                MaterialColors.getColor(
                    wrappedContext!!,
                    com.google.android.material.R.attr.colorTertiary,
                    -1
                )

                val colorAccent =
                    MaterialColors.getColor(
                        wrappedContext!!,
                        com.google.android.material.R.attr.colorAccent,
                        -1
                    )

                val backgroundProcessedColor = ColorUtils.getColor(
                    colorSurface,
                    ColorUtils.ColorType.COLOR_BACKGROUND_ELEVATED,
                    context)

                val surfaceTransition = ValueAnimator.ofArgb(
                    fullPlayerFinalColor,
                    backgroundProcessedColor
                )

                val primaryTransition = ValueAnimator.ofArgb(
                    colorPrimaryFinalColor,
                    colorPrimary
                )

                val secondaryContainerTransition = ValueAnimator.ofArgb(
                    colorSecondaryContainerFinalColor,
                    colorSecondaryContainer
                )

                val onSecondaryContainerTransition = ValueAnimator.ofArgb(
                    colorOnSecondaryContainerFinalColor,
                    colorOnSecondaryContainer
                )

                surfaceTransition.apply {
                    addUpdateListener { animation ->
                        fullPlayer.setBackgroundColor(
                            animation.animatedValue as Int
                        )
                        bottomSheetFullLyricRecyclerView.setBackgroundColor(
                            animation.animatedValue as Int
                        )
                    }
                    duration = BACKGROUND_COLOR_TRANSITION_SEC
                }

                primaryTransition.apply {
                    addUpdateListener { animation ->
                        val progressColor = animation.animatedValue as Int
                        bottomSheetFullSlider.thumbTintList =
                            ColorStateList.valueOf(progressColor)
                        bottomSheetFullSlider.trackActiveTintList =
                            ColorStateList.valueOf(progressColor)
                        bottomSheetFullSeekBar.progressTintList =
                            ColorStateList.valueOf(progressColor)
                        bottomSheetFullSeekBar.thumbTintList =
                            ColorStateList.valueOf(progressColor)
                    }
                    duration = BACKGROUND_COLOR_TRANSITION_SEC
                }

                secondaryContainerTransition.apply {
                    addUpdateListener { animation ->
                        val progressColor = animation.animatedValue as Int
                        bottomSheetFullSeekBar.secondaryProgressTintList =
                            ColorStateList.valueOf(progressColor)
                        bottomSheetFullControllerButton.backgroundTintList =
                            ColorStateList.valueOf(progressColor)
                    }
                    duration = BACKGROUND_COLOR_TRANSITION_SEC
                }

                onSecondaryContainerTransition.apply {
                    addUpdateListener { animation ->
                        val progressColor = animation.animatedValue as Int
                        bottomSheetFullControllerButton.iconTint =
                            ColorStateList.valueOf(progressColor)
                    }
                    duration = BACKGROUND_COLOR_TRANSITION_SEC
                }

                withContext(Dispatchers.Main) {
                    surfaceTransition.start()
                    primaryTransition.start()
                    secondaryContainerTransition.start()
                    onSecondaryContainerTransition.start()
                }

                delay(FOREGROUND_COLOR_TRANSITION_SEC)
                fullPlayerFinalColor = backgroundProcessedColor
                colorPrimaryFinalColor = colorPrimary
                colorSecondaryContainerFinalColor = colorSecondaryContainer
                colorOnSecondaryContainerFinalColor = colorOnSecondaryContainer

                withContext(Dispatchers.Main) {
                    bottomSheetFullTitle.setTextColor(
                        colorOnSurface
                    )
                    bottomSheetFullSubtitle.setTextColor(
                        colorOnSurfaceVariant
                    )
                    bottomSheetFullCoverFrame.setCardBackgroundColor(
                        colorSurface
                    )
                    bottomSheetFullLyricAdapter.updateTextColor(
                        ColorUtils.getColor(
                            colorSecondaryContainerFinalColor,
                            ColorUtils.ColorType.COLOR_CONTRAST_FAINTED,
                            context
                        ),
                        colorPrimary
                    )

                    bottomSheetFullSlider.trackInactiveTintList =
                        ColorStateList.valueOf(colorSurfaceContainerHighest)

                    bottomSheetTimerButton.iconTint =
                        selectorBackground
                    bottomSheetPlaylistButton.iconTint =
                        selectorBackground
                    bottomSheetShuffleButton.iconTint =
                        selectorBackground
                    bottomSheetLoopButton.iconTint =
                        selectorBackground
                    bottomSheetLyricButton.iconTint =
                        selectorBackground
                    bottomSheetFavoriteButton.iconTint =
                        selectorFavBackground

                    bottomSheetFullNextButton.iconTint =
                        ColorStateList.valueOf(colorOnSurface)
                    bottomSheetFullPreviousButton.iconTint =
                        ColorStateList.valueOf(colorOnSurface)
                    bottomSheetFullSlideUpButton.iconTint =
                        ColorStateList.valueOf(colorOnSurface)

                    bottomSheetFullPosition.setTextColor(
                        colorAccent
                    )
                    bottomSheetFullDuration.setTextColor(
                        colorAccent
                    )

                    val gradientDrawableUp = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(backgroundProcessedColor, Color.TRANSPARENT)
                    )
                    gradientDrawableUp.gradientType = GradientDrawable.LINEAR_GRADIENT
                    val gradientDrawableDown = GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(backgroundProcessedColor, Color.TRANSPARENT)
                    )
                    gradientDrawableDown.gradientType = GradientDrawable.LINEAR_GRADIENT
                    bottomSheetFullLyricGradientViewUp.background = gradientDrawableUp
                    bottomSheetFullLyricGradientViewDown.background = gradientDrawableDown
                }
            } catch (e: Exception) {
                if (e is FileNotFoundException) {
                    removeColorScheme(false)
                    withContext(Dispatchers.Main) {
                        bottomSheetFullCover.setImageDrawable(
                            AppCompatResources.getDrawable(context, R.drawable.ic_default_cover)
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (instance.mediaItemCount != 0) {
            Glide
                .with(context)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_default_cover)
                .into(bottomSheetPreviewCover)
            Glide
                .with(context)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(
                    object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            bottomSheetFullCover.setImageDrawable(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // this is called when imageView is cleared on lifecycle call or for
                            // some other reason.
                            // if you are referencing the bitmap somewhere else too other than this imageView
                            // clear it here as you can no longer have the bitmap
                        }
                    }
                )
            if (!prefs.getBoolean("content_based_color", true)) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        MediaStore.Images.Media.getBitmap(
                            activity.contentResolver,
                            mediaItem?.mediaMetadata?.artworkUri
                        )
                    } catch (e: Exception) {
                        if (e is FileNotFoundException) {
                            withContext(Dispatchers.Main) {
                                bottomSheetFullCover.setImageDrawable(
                                    AppCompatResources.getDrawable(
                                        context,
                                        R.drawable.ic_default_cover
                                    )
                                )
                            }
                        }
                    }
                }
            }
            bottomSheetPreviewTitle.text = mediaItem?.mediaMetadata?.title
            bottomSheetPreviewSubtitle.text =
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist)
            bottomSheetFullTitle.setTextAnimation(mediaItem?.mediaMetadata?.title)
            bottomSheetFullSubtitle.setTextAnimation(
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist)
            )
            bottomSheetFullDuration.text =
                mediaItem?.mediaMetadata?.extras?.getLong("Duration")
                    ?.let { CalculationUtils.convertDurationToTimeStamp(it) }
            if (playlistNowPlaying != null) {
                playlistNowPlaying!!.text = mediaItem?.mediaMetadata?.title
                Glide
                    .with(context)
                    .load(mediaItem?.mediaMetadata?.artworkUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_default_cover)
                    .into(playlistNowPlayingCover!!)
            }

            if (Build.VERSION.SDK_INT >= 26 && prefs.getBoolean("content_based_color", true)) {
                addColorScheme()
            }

            if (activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition]
                    .songList.contains(instance.currentMediaItem)
            ) {
                // TODO
            } else {
                // TODO
            }
            try {
                val audioFile =
                    AudioFileIO.read(File(instance.currentMediaItem!!.getUri().toString()))
                val tag = audioFile.tag
                val lyrics = tag.getFirst(FieldKey.LYRICS)
                var parsedLyrics = MediaStoreUtils.parseLrcString(lyrics)
                if (lyrics != null && lyrics.isNotEmpty() &&
                    bottomSheetFullLyricList != parsedLyrics &&
                    parsedLyrics.isNotEmpty()
                ) {
                    bottomSheetFullLyricList.clear()
                    bottomSheetFullLyricList.add(MediaStoreUtils.Lyric())
                    bottomSheetFullLyricList.addAll(parsedLyrics)
                    bottomSheetFullLyricAdapter.notifyDataSetChanged()
                    resetToDefaultLyricPosition()
                } else if (parsedLyrics.isEmpty()) {
                    try {
                        val lrcFile = File(
                            instance.currentMediaItem!!.getUri().toString()
                                .substringBeforeLast('.') + ".lrc"
                        )
                        val stringBuilder = StringBuilder()
                        lrcFile.forEachLine {
                            stringBuilder.append(it).append("\n")
                        }
                        parsedLyrics = MediaStoreUtils.parseLrcString(stringBuilder.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    bottomSheetFullLyricList.clear()
                    if (parsedLyrics.isEmpty()) {
                        bottomSheetFullLyricList.add(
                            MediaStoreUtils.Lyric(
                                0,
                                context.getString(R.string.no_lyric_found)
                            )
                        )
                    } else if (parsedLyrics != bottomSheetFullLyricList) {
                        bottomSheetFullLyricList.add(MediaStoreUtils.Lyric())
                        bottomSheetFullLyricList.addAll(parsedLyrics)
                    }
                    bottomSheetFullLyricAdapter.notifyDataSetChanged()
                    resetToDefaultLyricPosition()
                }
            } catch (e: Exception) {
                bottomSheetFullLyricList.clear()
                bottomSheetFullLyricList.add(
                    MediaStoreUtils.Lyric(
                        0,
                        context.getString(R.string.music_format_not_supported)
                    )
                )
                bottomSheetFullLyricAdapter.notifyDataSetChanged()
                resetToDefaultLyricPosition()
            }
        }
        var newState = standardBottomSheetBehavior!!.state
        if (instance.mediaItemCount != 0 && visible) {
            if (newState != BottomSheetBehavior.STATE_EXPANDED) {
                newState = BottomSheetBehavior.STATE_COLLAPSED
            }
        } else {
            newState = BottomSheetBehavior.STATE_HIDDEN
        }
        handler.post {
            if (!waitedForContainer) {
                waitedForContainer = true
                standardBottomSheetBehavior!!.setStateWithoutAnimation(newState)
            } else standardBottomSheetBehavior!!.state = newState
        }
        val position = CalculationUtils.convertDurationToTimeStamp(instance.currentPosition)
        val duration = instance.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
        if (duration != null && !isUserTracking) {
            bottomSheetFullSeekBar.max = duration.toInt()
            bottomSheetFullSeekBar.progress = instance.currentPosition.toInt()
            bottomSheetFullSlider.valueTo = duration.toFloat()
            bottomSheetFullSlider.value = instance.currentPosition.toFloat()
            bottomSheetFullPosition.text = position
        }
        updateLyric(duration)
    }

    private val sessionListener: MediaController.Listener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (command.customAction == GramophonePlaybackService.SERVICE_TIMER_CHANGED) {
                bottomSheetTimerButton.isChecked = controller.hasTimer()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        sessionToken =
            SessionToken(context, ComponentName(context, GramophonePlaybackService::class.java))
        controllerFuture =
            MediaController
                .Builder(context, sessionToken!!)
                .setListener(sessionListener)
                .buildAsync()
        controllerFuture!!.addListener(
            {
                instance.addListener(this)
                bottomSheetTimerButton.isChecked = instance.hasTimer()
                onRepeatModeChanged(instance.repeatMode)
                onShuffleModeEnabledChanged(instance.shuffleModeEnabled)
                onIsPlayingChanged(instance.isPlaying)
                onMediaItemTransition(
                    instance.currentMediaItem,
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                )
                if (activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition]
                        .songList.contains(instance.currentMediaItem)) {
                    bottomSheetFavoriteButton.isChecked = true
                    // TODO
                } else {
                    bottomSheetFavoriteButton.isChecked = false
                    // TODO
                }
                handler.post { ready = true }
            },
            MoreExecutors.directExecutor(),
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (controllerFuture?.isDone == true) {
            instance.removeListener(this)
            instance.release()
        }
        controllerFuture?.cancel(true)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        bottomSheetShuffleButton.isChecked = shuffleModeEnabled
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        when (repeatMode) {
            Player.REPEAT_MODE_ALL -> {
                bottomSheetLoopButton.isChecked = true
                bottomSheetLoopButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.ic_repeat)
            }

            Player.REPEAT_MODE_ONE -> {
                bottomSheetLoopButton.isChecked = true
                bottomSheetLoopButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.ic_repeat_one)
            }

            Player.REPEAT_MODE_OFF -> {
                bottomSheetLoopButton.isChecked = false
                bottomSheetLoopButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.ic_repeat)
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (bottomSheetPreviewControllerButton.getTag(R.id.play_next) as Int? != 1) {
                bottomSheetPreviewControllerButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.play_anim)
                bottomSheetFullControllerButton.icon =
                    AppCompatResources.getDrawable(
                        if (wrappedContext != null) wrappedContext!! else context,
                        R.drawable.play_anim)
                bottomSheetFullControllerButton.background =
                    AppCompatResources.getDrawable(context, R.drawable.bg_play_anim)
                bottomSheetFullControllerButton.icon.startAnimation()
                bottomSheetFullControllerButton.background.startAnimation()
                bottomSheetPreviewControllerButton.icon.startAnimation()
                bottomSheetPreviewControllerButton.setTag(R.id.play_next, 1)
            }
            if (!isUserTracking) {
                progressDrawable.animate = true
            }
            if (!runnableRunning) {
                handler.postDelayed(positionRunnable, instance.currentPosition % 1000)
                runnableRunning = true
            }
        } else if (instance.playbackState != Player.STATE_BUFFERING) {
            if (bottomSheetPreviewControllerButton.getTag(R.id.play_next) as Int? != 2) {
                bottomSheetPreviewControllerButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.pause_anim)
                bottomSheetFullControllerButton.icon =
                    AppCompatResources.getDrawable(
                        if (wrappedContext != null) wrappedContext!! else context,
                        R.drawable.pause_anim)
                bottomSheetFullControllerButton.background =
                    AppCompatResources.getDrawable(context, R.drawable.bg_pause_anim)
                bottomSheetFullControllerButton.icon.startAnimation()
                bottomSheetFullControllerButton.background.startAnimation()
                bottomSheetPreviewControllerButton.icon.startAnimation()
                bottomSheetPreviewControllerButton.setTag(R.id.play_next, 2)
            }
            if (!isUserTracking) {
                progressDrawable.animate = false
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //android.util.Log.e("hi","$keyCode") TODO
        if (controllerFuture?.isDone != true || controllerFuture?.isCancelled != false)
            return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                instance.playOrPause(); true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                instance.seekToPreviousMediaItem(); true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                instance.seekToNextMediaItem(); true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun dumpPlaylist(): MutableList<MediaItem> {
        val items = mutableListOf<MediaItem>()
        for (i in 0 until instance.mediaItemCount) {
            items.add(instance.getMediaItemAt(i))
        }
        return items
    }

    class LyricAdapter(
        private val lyricList: MutableList<MediaStoreUtils.Lyric>,
        private val activity: MainActivity
    ) : RecyclerView.Adapter<LyricAdapter.ViewHolder>() {

        private var defaultTextColor = MaterialColors.getColor(
            activity,
            com.google.android.material.R.attr.colorPrimaryVariant,
            -1
        )

        private var highlightTextColor = MaterialColors.getColor(
            activity,
            com.google.android.material.R.attr.colorPrimary,
            -1
        )

        var currentBoldPos = -1
        private var currentTranslationPos = -1

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): LyricAdapter.ViewHolder =
            ViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.lyrics, parent, false),
            )

        override fun onBindViewHolder(holder: LyricAdapter.ViewHolder, position: Int) {
            val lyric = lyricList[position]

            with(holder.lyricCard) {
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= 23) {
                        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                    val instance = activity.getPlayer()
                    if (!instance.isPlaying) {
                        instance.play()
                    }
                    instance.seekTo(lyric.timeStamp)
                }
            }

            with(holder.lyricTextView) {
                visibility = if (lyric.content.isNotEmpty()) View.VISIBLE else View.GONE
                text = lyric.content

                val textSize = if (lyric.isTranslation) 20f else 28f
                val paddingTop = if (lyric.isTranslation) 2.px else 18.px
                val paddingBottom = if (position + 1 < lyricList.size &&
                    lyricList[position + 1].isTranslation) 2.px else 18.px

                this.textSize = textSize
                setPadding(10.px, paddingTop, 10.px, paddingBottom)

                val isBold = position == currentBoldPos || position == currentTranslationPos
                typeface = if (isBold) Typeface.defaultFromStyle(Typeface.BOLD) else Typeface.DEFAULT
                setTextColor(if (isBold) highlightTextColor else defaultTextColor)
            }
        }

        override fun getItemCount(): Int = lyricList.size

        inner class ViewHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {
            val lyricTextView: TextView = view.findViewById(R.id.lyric)
            val lyricCard: MaterialCardView = view.findViewById(R.id.cardview)
        }

        fun updateTextColor(newColor: Int, newHighlightColor: Int) {
            defaultTextColor = newColor
            highlightTextColor = newHighlightColor
            notifyDataSetChanged()
        }

        fun updateHighlight(position: Int) {
            if (currentBoldPos == position) return
            if (position >= 0) {
                currentBoldPos.let {
                    notifyItemChanged(it)
                    currentBoldPos = position
                    notifyItemChanged(currentBoldPos)
                }

                if (position + 1 < lyricList.size &&
                    lyricList[position + 1].isTranslation) {
                    currentTranslationPos.let {
                        notifyItemChanged(it)
                        currentTranslationPos = position + 1
                        notifyItemChanged(currentTranslationPos)
                    }
                } else if (currentTranslationPos != -1) {
                    notifyItemChanged(currentTranslationPos)
                    currentTranslationPos = -1
                }
            } else {
                currentBoldPos = -1
                currentTranslationPos = -1
            }
        }
    }


    class PlaylistCardAdapter(
        private val playlist: MutableList<MediaItem>,
        private val activity: MainActivity
    ) : RecyclerView.Adapter<PlaylistCardAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PlaylistCardAdapter.ViewHolder =
            ViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.adapter_list_card_playlist, parent, false),
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.songName.text = playlist[holder.bindingAdapterPosition].mediaMetadata.title
            holder.songArtist.text = playlist[holder.bindingAdapterPosition].mediaMetadata.artist
            holder.indicator.text =
                CalculationUtils.convertDurationToTimeStamp(
                    playlist[holder.bindingAdapterPosition].mediaMetadata.extras?.getLong("Duration")!!
                )
            Glide
                .with(holder.songCover.context)
                .load(playlist[position].mediaMetadata.artworkUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_default_cover)
                .into(holder.songCover)
            holder.closeButton.setOnClickListener {
                if (Build.VERSION.SDK_INT >= 23) {
                    it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                val instance = activity.getPlayer()
                val pos = holder.bindingAdapterPosition
                playlist.removeAt(pos)
                notifyItemRemoved(pos)
                instance.removeMediaItem(pos)
            }
            holder.itemView.setOnClickListener {
                if (Build.VERSION.SDK_INT >= 23) {
                    it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                val instance = activity.getPlayer()
                instance.seekToDefaultPosition(holder.absoluteAdapterPosition)
            }
        }

        override fun getItemCount(): Int = playlist.size

        inner class ViewHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val songName: TextView = view.findViewById(R.id.title)
            val songArtist: TextView = view.findViewById(R.id.artist)
            val songCover: ImageView = view.findViewById(R.id.cover)
            val indicator: TextView = view.findViewById(R.id.indicator)
            val closeButton: MaterialButton = view.findViewById(R.id.close)
        }

    }

    @Suppress("DEPRECATION")
    private fun insertIntoPlaylist(song: MediaItem) {
        val playlistEntry = ContentValues()
        val playlistId = activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition].id
        playlistEntry.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID, playlistId)
        playlistEntry.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song.mediaId)

        context.contentResolver.insert(
            MediaStore.Audio.Playlists.Members.getContentUri(
                "external",
                playlistId
            ), playlistEntry
        )
        activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition].songList.add(song)
    }

    @Suppress("DEPRECATION")
    private fun removeFromPlaylist(song: MediaItem) {
        val selection = "${MediaStore.Audio.Playlists.Members.AUDIO_ID} = ?"
        val selectionArgs = arrayOf(song.mediaId)
        val playlistId = activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition].id

        context.contentResolver.delete(
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
            selection,
            selectionArgs
        )
        activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition].songList.remove(song)
    }


}