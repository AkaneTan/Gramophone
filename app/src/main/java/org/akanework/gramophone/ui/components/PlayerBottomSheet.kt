package org.akanework.gramophone.ui.components

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.GramophonePlaybackService
import org.akanework.gramophone.logic.getTimer
import org.akanework.gramophone.logic.hasTimer
import org.akanework.gramophone.logic.setTimer
import org.akanework.gramophone.logic.utils.GramophoneUtils
import org.akanework.gramophone.logic.utils.MyBottomSheetBehavior
import org.akanework.gramophone.logic.utils.playOrPause
import org.akanework.gramophone.logic.utils.startAnimation
import org.akanework.gramophone.ui.MainActivity

class PlayerBottomSheet private constructor(
    context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes),
    Player.Listener, DefaultLifecycleObserver {
    constructor(context: Context, attributeSet: AttributeSet?)
            : this(context, attributeSet, 0, 0)

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
    private val bottomSheetLyricButton: MaterialButton
    private val bottomSheetTimerButton: MaterialButton
    private val bottomSheetFullSeekBar: SeekBar
    private val bottomSheetFullSlider: Slider
    private var standardBottomSheetBehavior: MyBottomSheetBehavior<FrameLayout>? = null
    private var bottomSheetBackCallback: OnBackPressedCallback? = null
    private val fullPlayer: View
    private val previewPlayer: View
    private val progressDrawable: SquigglyProgress
    private var isLegacyProgressEnabled: Boolean = false

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
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        isLegacyProgressEnabled = prefs.getBoolean("default_progress_bar", false)
        inflate(context, R.layout.bottom_sheet_impl, this)
        id = R.id.player_layout
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
        bottomSheetFullSeekBar = findViewById(R.id.slider_squiggly)
        bottomSheetFullSlider = findViewById(R.id.slider_vert)
        bottomSheetFullSlideUpButton = findViewById(R.id.slide_down)
        bottomSheetShuffleButton = findViewById(R.id.sheet_random)
        bottomSheetLoopButton = findViewById(R.id.sheet_loop)
        bottomSheetLyricButton = findViewById(R.id.lyrics)
        bottomSheetTimerButton = findViewById(R.id.timer)
        bottomSheetPlaylistButton = findViewById(R.id.playlist)
        previewPlayer = findViewById(R.id.preview_player)
        fullPlayer = findViewById(R.id.full_player)
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
                            GramophoneUtils.convertDurationToTimeStamp((progress.toLong()))
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
            instance.repeatMode = when (instance.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> throw IllegalStateException()
            }
        }

        bottomSheetPlaylistButton.setOnClickListener {
            val playlistBottomSheet = BottomSheetDialog(context)
            playlistBottomSheet.setContentView(R.layout.playlist_bottom_sheet)
            val recyclerView = playlistBottomSheet.findViewById<RecyclerView>(R.id.recyclerview)!!
            val playlistAdapter = PlaylistCardAdapter(dumpPlaylist(), instance)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = playlistAdapter
            recyclerView.scrollToPosition(instance.currentMediaItemIndex)
            FastScrollerBuilder(recyclerView).useMd2Style().build()
            playlistBottomSheet.show()
        }

        bottomSheetPreviewControllerButton.setOnClickListener {
            instance.playOrPause()
        }
        bottomSheetFullControllerButton.setOnClickListener {
            instance.playOrPause()
        }
        bottomSheetPreviewNextButton.setOnClickListener {
            instance.seekToNextMediaItem()
        }
        bottomSheetFullPreviousButton.setOnClickListener {
            instance.seekToPreviousMediaItem()
        }
        bottomSheetFullNextButton.setOnClickListener {
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
                        GramophoneUtils.convertDurationToTimeStamp((value).toLong())
                }
            }
        }

        bottomSheetFullSeekBar.setOnSeekBarChangeListener(touchListener)
        bottomSheetFullSlider.addOnSliderTouchListener(touchListener)

        bottomSheetFullSlideUpButton.setOnClickListener {
            standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }
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

    private val positionRunnable = object : Runnable {
        override fun run() {
            val position =
                GramophoneUtils.convertDurationToTimeStamp(instance.currentPosition)
            if (runnableRunning) {
                val duration = instance.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
                if (duration != null && !isUserTracking) {
                    bottomSheetFullSeekBar.max = duration.toInt()
                    bottomSheetFullSeekBar.progress = instance.currentPosition.toInt()
                    bottomSheetFullSlider.valueTo = duration.toFloat()
                    bottomSheetFullSlider.value = instance.currentPosition.toFloat()
                    bottomSheetFullPosition.text = position
                }
            }
            if (instance.isPlaying) {
                handler.postDelayed(this, instance.currentPosition % 1000)
            } else {
                runnableRunning = false
            }
        }
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        post {
            standardBottomSheetBehavior = MyBottomSheetBehavior.from(this)
            standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    standardBottomSheetBehavior!!.startBackProgress(backEvent)
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    standardBottomSheetBehavior!!.updateBackProgress(backEvent)
                }

                override fun handleOnBackPressed() {
                    standardBottomSheetBehavior!!.handleBackInvoked()
                }

                override fun handleOnBackCancelled() {
                    standardBottomSheetBehavior!!.cancelBackProgress()
                }
            }
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

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (instance.mediaItemCount != 0) {
            Glide
                .with(bottomSheetPreviewCover)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .placeholder(R.drawable.ic_default_cover)
                .into(bottomSheetPreviewCover)
            Glide
                .with(bottomSheetFullCover)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .placeholder(R.drawable.ic_default_cover)
                .into(bottomSheetFullCover)
            bottomSheetPreviewTitle.text = mediaItem?.mediaMetadata?.title
            bottomSheetPreviewSubtitle.text =
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist)
            bottomSheetFullTitle.text = mediaItem?.mediaMetadata?.title
            bottomSheetFullSubtitle.text =
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist)
            bottomSheetFullDuration.text =
                mediaItem?.mediaMetadata?.extras?.getLong("Duration")
                    ?.let { GramophoneUtils.convertDurationToTimeStamp(it) }
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
        val position = GramophoneUtils.convertDurationToTimeStamp(instance.currentPosition)
        val duration = instance.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
        if (duration != null && !isUserTracking) {
            bottomSheetFullSeekBar.max = duration.toInt()
            bottomSheetFullSeekBar.progress = instance.currentPosition.toInt()
            bottomSheetFullSlider.valueTo = duration.toFloat()
            bottomSheetFullSlider.value = instance.currentPosition.toFloat()
            bottomSheetFullPosition.text = position
        }
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
                    AppCompatResources.getDrawable(context, R.drawable.play_anim)
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
                    AppCompatResources.getDrawable(context, R.drawable.pause_anim)
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

    class PlaylistCardAdapter(
        private val playlist: MutableList<MediaItem>,
        private val instance: MediaController
    ) : RecyclerView.Adapter<PlaylistCardAdapter.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PlaylistCardAdapter.ViewHolder =
            ViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.adapter_list_card_smaller, parent, false),
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.songName.text = playlist[holder.bindingAdapterPosition].mediaMetadata.title
            holder.songArtist.text = playlist[holder.bindingAdapterPosition].mediaMetadata.artist
            Glide
                .with(holder.songCover.context)
                .load(playlist[position].mediaMetadata.artworkUri)
                .placeholder(R.drawable.ic_default_cover)
                .into(holder.songCover)
            holder.closeButton.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                playlist.removeAt(pos)
                notifyItemRemoved(pos)
                instance.removeMediaItem(pos)
            }
        }

        override fun getItemCount(): Int = playlist.size

        inner class ViewHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val songName: TextView = view.findViewById(R.id.title)
            val songArtist: TextView = view.findViewById(R.id.artist)
            val songCover: ImageView = view.findViewById(R.id.cover)
            val closeButton: MaterialButton = view.findViewById(R.id.close)
        }

    }
}