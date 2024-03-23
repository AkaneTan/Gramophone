package org.akanework.gramophone.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.GramophonePlaybackService
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.fadInAnimation
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.getIntStrict
import org.akanework.gramophone.logic.getLyrics
import org.akanework.gramophone.logic.getTimer
import org.akanework.gramophone.logic.hasTimer
import org.akanework.gramophone.logic.playOrPause
import org.akanework.gramophone.logic.setTextAnimation
import org.akanework.gramophone.logic.setTimer
import org.akanework.gramophone.logic.startAnimation
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.logic.utils.CalculationUtils
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import kotlin.math.min

class FullBottomSheet(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
	ConstraintLayout(context, attrs, defStyleAttr, defStyleRes), Player.Listener,
	SharedPreferences.OnSharedPreferenceChangeListener {
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
		this(context, attrs, defStyleAttr, 0)
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
	constructor(context: Context) : this(context, null)

	private val activity
		get() = context as MainActivity
	private var controllerFuture: ListenableFuture<MediaController>? = null
	private val instance: MediaController?
		get() = if (controllerFuture?.isDone == false || controllerFuture?.isCancelled == true)
			null else controllerFuture?.get()
	var minimize: (() -> Unit)? = null

	private var wrappedContext: Context? = null
	private var currentJob: Job? = null
	private var isUserTracking = false
	private var runnableRunning = false
	private var firstTime = false
	private var lastArtworkUri: Uri? = null

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

	companion object {
		const val SLIDER_UPDATE_INTERVAL: Long = 100
		const val BACKGROUND_COLOR_TRANSITION_SEC: Long = 300
		const val FOREGROUND_COLOR_TRANSITION_SEC: Long = 150
		const val LYRIC_FADE_TRANSITION_SEC: Long = 125
	}

	private val touchListener = object : SeekBar.OnSeekBarChangeListener, Slider.OnSliderTouchListener {
		override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
			if (fromUser) {
				val dest = instance?.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
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
			val mediaId = instance?.currentMediaItem?.mediaId
			if (mediaId != null) {
				if (seekBar != null) {
					instance?.seekTo((seekBar.progress.toLong()))
					updateLyric(seekBar.progress.toLong())
				}
			}
			isUserTracking = false
			progressDrawable.animate = instance?.isPlaying == true || instance?.playWhenReady == true
		}

		override fun onStartTrackingTouch(slider: Slider) {
			isUserTracking = true
		}

		override fun onStopTrackingTouch(slider: Slider) {
			// This value is multiplied by 1000 is because
			// when the number is too big (like when toValue
			// used the duration directly) we might encounter
			// some performance problem.
			val mediaId = instance?.currentMediaItem?.mediaId
			if (mediaId != null) {
				instance?.seekTo((slider.value.toLong()))
				updateLyric(slider.value.toLong())
			}
			isUserTracking = false
		}
	}
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
	val bottomSheetLyricButton: MaterialButton
	private val bottomSheetFullSeekBar: SeekBar
	private val bottomSheetFullSlider: Slider
	private val bottomSheetFullCoverFrame: MaterialCardView
	val bottomSheetFullLyricRecyclerView: RecyclerView
	private val bottomSheetFullLyricList: MutableList<MediaStoreUtils.Lyric> = mutableListOf()
	private val bottomSheetFullLyricAdapter: LyricAdapter = LyricAdapter(bottomSheetFullLyricList)
	private val bottomSheetFullLyricLinearLayoutManager = LinearLayoutManager(context)
	private val progressDrawable: SquigglyProgress
	private var fullPlayerFinalColor: Int = -1
	private var colorPrimaryFinalColor: Int = -1
	private var colorSecondaryContainerFinalColor: Int = -1
	private var colorOnSecondaryContainerFinalColor: Int = -1
	private var colorContrastFaintedFinalColor: Int = -1
	private var playlistNowPlaying: TextView? = null
	private var playlistNowPlayingCover: ImageView? = null

	init {
		inflate(context, R.layout.full_player, this)
		bottomSheetFullCoverFrame = findViewById(R.id.album_cover_frame)
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
		bottomSheetTimerButton = findViewById(R.id.timer)
		bottomSheetFavoriteButton = findViewById(R.id.favor)
		bottomSheetPlaylistButton = findViewById(R.id.playlist)
		bottomSheetLyricButton = findViewById(R.id.lyrics)
		bottomSheetFullLyricRecyclerView = findViewById(R.id.lyric_frame)
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
		refreshSettings(null)
		prefs.registerOnSharedPreferenceChangeListener(this)

		val seekBarProgressWavelength =
			context.resources
				.getDimensionPixelSize(R.dimen.media_seekbar_progress_wavelength)
				.toFloat()
		val seekBarProgressAmplitude =
			context.resources
				.getDimensionPixelSize(R.dimen.media_seekbar_progress_amplitude)
				.toFloat()
		val seekBarProgressPhase =
			context.resources
				.getDimensionPixelSize(R.dimen.media_seekbar_progress_phase)
				.toFloat()
		val seekBarProgressStrokeWidth =
			context.resources
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

		bottomSheetTimerButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			val picker =
				MaterialTimePicker
					.Builder()
					.setHour((instance?.getTimer() ?: 0) / 3600 / 1000)
					.setMinute(((instance?.getTimer() ?: 0) % (3600 * 1000)) / (60 * 1000))
					.setTimeFormat(TimeFormat.CLOCK_24H)
					.setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
					.build()
			picker.addOnPositiveButtonClickListener {
				val destinationTime: Int = picker.hour * 1000 * 3600 + picker.minute * 1000 * 60
				instance?.setTimer(destinationTime)
			}
			picker.addOnDismissListener {
				bottomSheetTimerButton.isChecked = instance?.hasTimer() == true
			}
			picker.show(activity.supportFragmentManager, "timer")
		}

		bottomSheetLoopButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			instance?.repeatMode = when (instance?.repeatMode) {
				Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
				Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
				Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
				else -> throw IllegalStateException()
			}
		}

		/*
		bottomSheetFavoriteButton.addOnCheckedChangeListener { _, isChecked ->
			/*
			if (isChecked) {
				instance.currentMediaItem?.let { insertIntoPlaylist(it) }
			} else {
				instance.currentMediaItem?.let { removeFromPlaylist(it) }
			}
			 */
		}

		 */

		bottomSheetPlaylistButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			val playlistBottomSheet = BottomSheetDialog(context)
			playlistBottomSheet.setContentView(R.layout.playlist_bottom_sheet)
			val recyclerView = playlistBottomSheet.findViewById<MyRecyclerView>(R.id.recyclerview)!!
			ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, ic ->
				val i = ic.getInsets(WindowInsetsCompat.Type.systemBars()
						or WindowInsetsCompat.Type.displayCutout())
				v.setPadding(i.left, 0, i.right, i.bottom)
				return@setOnApplyWindowInsetsListener ic
			}
			val playlistAdapter = PlaylistCardAdapter(dumpPlaylist(), activity)
			playlistNowPlaying = playlistBottomSheet.findViewById(R.id.now_playing)
			playlistNowPlaying!!.text = instance?.currentMediaItem?.mediaMetadata?.title
			playlistNowPlayingCover = playlistBottomSheet.findViewById(R.id.now_playing_cover)
			Glide
				.with(context)
				.load(instance?.currentMediaItem?.mediaMetadata?.artworkUri)
				.transition(DrawableTransitionOptions.withCrossFade())
				.placeholder(R.drawable.ic_default_cover)
				.into(playlistNowPlayingCover!!)
			recyclerView.layoutManager = LinearLayoutManager(context)
			recyclerView.adapter = playlistAdapter
			recyclerView.scrollToPosition(instance?.currentMediaItemIndex ?: 0)
			recyclerView.fastScroll(null, null)
			playlistBottomSheet.setOnDismissListener {
				if (playlistNowPlaying != null) {
					Glide.with(context.applicationContext).clear(playlistNowPlayingCover!!)
					playlistNowPlayingCover = null
					playlistNowPlaying = null
				}
			}
			playlistBottomSheet.show()
		}
		bottomSheetFullControllerButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			instance?.playOrPause()
		}
		bottomSheetFullPreviousButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			instance?.seekToPreviousMediaItem()
		}
		bottomSheetFullNextButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			instance?.seekToNextMediaItem()
		}
		bottomSheetShuffleButton.addOnCheckedChangeListener { _, isChecked ->
			instance?.shuffleModeEnabled = isChecked
		}

		bottomSheetFullSlider.addOnChangeListener { _, value, isUser ->
			if (isUser) {
				val dest = instance?.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
				if (dest != null) {
					bottomSheetFullPosition.text =
						CalculationUtils.convertDurationToTimeStamp((value).toLong())
				}
			}
		}

		bottomSheetFullSeekBar.setOnSeekBarChangeListener(touchListener)
		bottomSheetFullSlider.addOnSliderTouchListener(touchListener)

		bottomSheetFullSlideUpButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			minimize?.invoke()
		}

		bottomSheetLyricButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
			bottomSheetFullLyricRecyclerView.fadInAnimation(LYRIC_FADE_TRANSITION_SEC)
		}

		bottomSheetShuffleButton.setOnClickListener {
			ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
		}

		bottomSheetFullLyricRecyclerView.layoutManager =
			bottomSheetFullLyricLinearLayoutManager
		bottomSheetFullLyricRecyclerView.adapter =
			bottomSheetFullLyricAdapter

		removeColorScheme()
	}

	val sessionListener: MediaController.Listener = object : MediaController.Listener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onCustomCommand(
			controller: MediaController,
			command: SessionCommand,
			args: Bundle
		): ListenableFuture<SessionResult> {
			when (command.customAction) {
				GramophonePlaybackService.SERVICE_TIMER_CHANGED -> {
					bottomSheetTimerButton.isChecked = controller.hasTimer()
				}

				GramophonePlaybackService.SERVICE_GET_LYRICS -> {
					val parsedLyrics = instance?.getLyrics()
					if (bottomSheetFullLyricList != parsedLyrics) {
						bottomSheetFullLyricList.clear()
						if (parsedLyrics?.isEmpty() != false) {
							bottomSheetFullLyricList.add(
								MediaStoreUtils.Lyric(
									0,
									context.getString(R.string.no_lyric_found)
								)
							)
						} else {
							bottomSheetFullLyricList.addAll(parsedLyrics)
						}
						bottomSheetFullLyricAdapter.notifyDataSetChanged()
						resetToDefaultLyricPosition()
					}
				}
			}
			return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == "color_accuracy" || key == "content_based_color") {
			if (DynamicColors.isDynamicColorAvailable() &&
				prefs.getBooleanStrict("content_based_color", true)) {
				addColorScheme()
			} else {
				removeColorScheme()
			}
		} else {
			refreshSettings(key)
			if (key == "lyric_center" || key == "lyric_bold" || key == "lyric_contrast") {
				@Suppress("NotifyDataSetChanged")
				bottomSheetFullLyricAdapter.notifyDataSetChanged()
			}
		}
	}

	private fun refreshSettings(key: String?) {
		if (key == null || key == "default_progress_bar") {
			if (prefs.getBooleanStrict("default_progress_bar", false)) {
				bottomSheetFullSlider.visibility = View.VISIBLE
				bottomSheetFullSeekBar.visibility = View.GONE
			} else {
				bottomSheetFullSlider.visibility = View.GONE
				bottomSheetFullSeekBar.visibility = View.VISIBLE
			}
		}
		if (key == null || key == "centered_title") {
			if (prefs.getBooleanStrict("centered_title", true)) {
				bottomSheetFullTitle.gravity = Gravity.CENTER
				bottomSheetFullSubtitle.gravity = Gravity.CENTER
			} else {
				bottomSheetFullTitle.gravity = Gravity.CENTER_HORIZONTAL or Gravity.START
				bottomSheetFullSubtitle.gravity = Gravity.CENTER_HORIZONTAL or Gravity.START
			}
		}
		if (key == null || key == "bold_title") {
			if (prefs.getBooleanStrict("bold_title", true)) {
				bottomSheetFullTitle.typeface = TypefaceCompat.create(context, null, 700, false)
			} else {
				bottomSheetFullTitle.typeface = TypefaceCompat.create(context, null, 500, false)
			}
		}
		if (key == null || key == "album_round_corner") {
			bottomSheetFullCoverFrame.radius = prefs.getIntStrict(
				"album_round_corner",
				context.resources.getInteger(R.integer.round_corner_radius)
			).dpToPx(context).toFloat()
		}
		if (key == null || key == "lyric_center" || key == "lyric_bold" || key == "lyric_contrast") {
			bottomSheetFullLyricAdapter.updateLyricStatus()
		}
	}

	fun onStart(cf: ListenableFuture<MediaController>) {
		controllerFuture = cf
		controllerFuture!!.addListener({
			firstTime = true
			instance?.addListener(this)
			bottomSheetTimerButton.isChecked = instance?.hasTimer() == true
			onRepeatModeChanged(instance?.repeatMode ?: Player.REPEAT_MODE_OFF)
			onShuffleModeEnabledChanged(instance?.shuffleModeEnabled ?: false)
			onPlaybackStateChanged(instance?.playbackState ?: Player.STATE_IDLE)
			onMediaItemTransition(
				instance?.currentMediaItem,
				Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
			)
			firstTime = false
			/*
			if (activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition]
					.songList.contains(instance.currentMediaItem)) {
				bottomSheetFavoriteButton.isChecked = true
				// TODO
			} else {
				bottomSheetFavoriteButton.isChecked = false
				// TODO
			}

			 */
		}, MoreExecutors.directExecutor())
	}

	fun onStop() {
		runnableRunning = false
		instance?.removeListener(this)
		controllerFuture = null
	}

	override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
		super.setPadding(left, top, right, bottom)
		// this is to enable edge to edge for lyric view with some trickery
		bottomSheetFullLyricRecyclerView.updateLayoutParams<MarginLayoutParams> {
			topMargin = -top
			bottomMargin = -bottom
		}
		bottomSheetFullLyricRecyclerView.setPadding(0, top, 0, bottom)
	}

	private fun removeColorScheme() {
		currentJob?.cancel()
		wrappedContext = null
		currentJob = CoroutineScope(Dispatchers.Default).launch {
			applyColorScheme()
		}
	}

	private fun addColorScheme() {
		currentJob?.cancel()
		currentJob = CoroutineScope(Dispatchers.Default).launch {
			var drawable = bottomSheetFullCover.drawable
			if (drawable is TransitionDrawable) drawable = drawable.getDrawable(1)
			val bitmap = if (drawable is BitmapDrawable) drawable.bitmap else {
				removeColorScheme()
				return@launch
			}
			val colorAccuracy = prefs.getBoolean("color_accuracy", false)
			val targetWidth = if (colorAccuracy) bitmap.width / 4 else bitmap.width / 16
			val targetHeight = if (colorAccuracy) bitmap.height / 4 else bitmap.width / 16
			val scaledBitmap =
				Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

			wrappedContext = DynamicColors.wrapContextIfAvailable(
				context,
				DynamicColorsOptions.Builder()
					.setContentBasedSource(scaledBitmap)
					.build()
			)

			applyColorScheme()
		}
	}

	private suspend fun applyColorScheme() {
		val ctx = wrappedContext ?: context

		val colorSurface = MaterialColors.getColor(
			ctx,
			com.google.android.material.R.attr.colorSurface,
			-1
		)

		val colorOnSurface = MaterialColors.getColor(
			ctx,
			com.google.android.material.R.attr.colorOnSurface,
			-1
		)

		val colorOnSurfaceVariant = MaterialColors.getColor(
			ctx,
			com.google.android.material.R.attr.colorOnSurfaceVariant,
			-1
		)

		val colorPrimary =
			MaterialColors.getColor(
				ctx,
				com.google.android.material.R.attr.colorPrimary,
				-1
			)

		val colorSecondaryContainer =
			MaterialColors.getColor(
				ctx,
				com.google.android.material.R.attr.colorSecondaryContainer,
				-1
			)

		val colorOnSecondaryContainer =
			MaterialColors.getColor(
				ctx,
				com.google.android.material.R.attr.colorOnSecondaryContainer,
				-1
			)

		val selectorBackground =
			AppCompatResources.getColorStateList(
				ctx,
				R.color.sl_check_button
			)

		val selectorFavBackground =
			AppCompatResources.getColorStateList(
				ctx,
				R.color.sl_fav_button
			)

		val colorAccent =
			MaterialColors.getColor(
				ctx,
				com.google.android.material.R.attr.colorAccent,
				-1
			)

		val backgroundProcessedColor = ColorUtils.getColor(
			colorSurface,
			ColorUtils.ColorType.COLOR_BACKGROUND_ELEVATED,
			ctx
		)

		val colorContrastFainted = ColorUtils.getColor(
			colorSecondaryContainer,
			ColorUtils.ColorType.COLOR_CONTRAST_FAINTED,
			ctx
		)

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

		val colorContrastFaintedTransition = ValueAnimator.ofArgb(
			colorContrastFaintedFinalColor,
			colorContrastFainted
		)

		surfaceTransition.apply {
			addUpdateListener { animation ->
				setBackgroundColor(
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

		colorContrastFaintedTransition.apply {
			addUpdateListener { animation ->
				val progressColor = animation.animatedValue as Int
				bottomSheetFullSlider.trackInactiveTintList =
					ColorStateList.valueOf(progressColor)
			}
		}

		withContext(Dispatchers.Main) {
			surfaceTransition.start()
			primaryTransition.start()
			secondaryContainerTransition.start()
			onSecondaryContainerTransition.start()
			colorContrastFaintedTransition.start()
		}

		delay(FOREGROUND_COLOR_TRANSITION_SEC)
		fullPlayerFinalColor = backgroundProcessedColor
		colorPrimaryFinalColor = colorPrimary
		colorSecondaryContainerFinalColor = colorSecondaryContainer
		colorOnSecondaryContainerFinalColor = colorOnSecondaryContainer
		colorContrastFaintedFinalColor = colorContrastFainted

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
				colorOnSurfaceVariant,
				colorContrastFainted,
				colorPrimary
			)

			bottomSheetTimerButton.iconTint =
				ColorStateList.valueOf(colorOnSurface)
			bottomSheetPlaylistButton.iconTint =
				ColorStateList.valueOf(colorOnSurface)
			bottomSheetShuffleButton.iconTint =
				selectorBackground
			bottomSheetLoopButton.iconTint =
				selectorBackground
			bottomSheetLyricButton.iconTint =
				ColorStateList.valueOf(colorOnSurface)
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
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onMediaItemTransition(
		mediaItem: MediaItem?,
		reason: Int
	) {
		if (instance?.mediaItemCount != 0) {
			val artworkUri = mediaItem?.mediaMetadata?.artworkUri
			Glide
				.with(context)
				.load(artworkUri)
				.error(R.drawable.ic_default_cover)
				.into(object : CustomTarget<Drawable>() {
					override fun onResourceReady(
						resource: Drawable,
						transition: Transition<in Drawable>?
					) {
						bottomSheetFullCover.setImageDrawable(resource)
						if (DynamicColors.isDynamicColorAvailable() &&
							prefs.getBooleanStrict("content_based_color", true)
						) {
							addColorScheme()
						}
					}

					override fun onLoadFailed(errorDrawable: Drawable?) {
						removeColorScheme()
						bottomSheetFullCover.setImageDrawable(errorDrawable)
					}

					override fun onLoadCleared(placeholder: Drawable?) {
						// TODO
					}
				})
			lastArtworkUri = artworkUri
			bottomSheetFullTitle.setTextAnimation(mediaItem?.mediaMetadata?.title, skipAnimation = firstTime)
			bottomSheetFullSubtitle.setTextAnimation(
				mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist), skipAnimation = firstTime
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

			/*
			if (activity.libraryViewModel.playlistList.value!![MediaStoreUtils.favPlaylistPosition]
					.songList.contains(instance.currentMediaItem)
			) {
				// TODO
			} else {
				// TODO
			}

			 */
		} else {
			lastArtworkUri = null
			Glide.with(context.applicationContext).clear(bottomSheetFullCover)
			if (playlistNowPlaying != null) {
				Glide.with(context.applicationContext).clear(playlistNowPlayingCover!!)
			}
		}
		val position = CalculationUtils.convertDurationToTimeStamp(instance?.currentPosition ?: 0)
		val duration = instance?.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
		if (duration != null && !isUserTracking) {
			bottomSheetFullSeekBar.max = duration.toInt()
			bottomSheetFullSeekBar.progress = instance?.currentPosition?.toInt() ?: 0
			bottomSheetFullSlider.valueTo = duration.toFloat()
			bottomSheetFullSlider.value =
				min(instance?.currentPosition?.toFloat() ?: 0f, bottomSheetFullSlider.valueTo)
			bottomSheetFullPosition.text = position
		}
		updateLyric(duration)
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
		onPlaybackStateChanged(instance?.playbackState ?: Player.STATE_IDLE)
	}

	override fun onPlaybackStateChanged(playbackState: Int) {
		if (instance?.isPlaying == true) {
			if (bottomSheetFullControllerButton.getTag(R.id.play_next) as Int? != 1) {
				bottomSheetFullControllerButton.icon =
					AppCompatResources.getDrawable(
						wrappedContext ?: context,
						R.drawable.play_anim
					)
				bottomSheetFullControllerButton.background =
					AppCompatResources.getDrawable(context, R.drawable.bg_play_anim)
				bottomSheetFullControllerButton.icon.startAnimation()
				bottomSheetFullControllerButton.background.startAnimation()
				bottomSheetFullControllerButton.setTag(R.id.play_next, 1)
			}
			if (!isUserTracking) {
				progressDrawable.animate = true
			}
			if (!runnableRunning) {
				handler.postDelayed(positionRunnable, SLIDER_UPDATE_INTERVAL)
				runnableRunning = true
			}
		} else if (playbackState != Player.STATE_BUFFERING) {
			if (bottomSheetFullControllerButton.getTag(R.id.play_next) as Int? != 2) {
				bottomSheetFullControllerButton.icon =
					AppCompatResources.getDrawable(
						wrappedContext ?: context,
						R.drawable.pause_anim
					)
				bottomSheetFullControllerButton.background =
					AppCompatResources.getDrawable(context, R.drawable.bg_pause_anim)
				bottomSheetFullControllerButton.icon.startAnimation()
				bottomSheetFullControllerButton.background.startAnimation()
				bottomSheetFullControllerButton.setTag(R.id.play_next, 2)
			}
			if (!isUserTracking) {
				progressDrawable.animate = false
			}
		}
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		//android.util.Log.e("hi","$keyCode") TODO this method is no-op, but why?
		return when (keyCode) {
			KeyEvent.KEYCODE_SPACE -> {
				instance?.playOrPause(); true
			}

			KeyEvent.KEYCODE_DPAD_LEFT -> {
				instance?.seekToPreviousMediaItem(); true
			}

			KeyEvent.KEYCODE_DPAD_RIGHT -> {
				instance?.seekToNextMediaItem(); true
			}

			else -> super.onKeyDown(keyCode, event)
		}
	}

	private fun dumpPlaylist(): MutableList<MediaItem> {
		val items = mutableListOf<MediaItem>()
		for (i in 0 until instance!!.mediaItemCount) {
			items.add(instance!!.getMediaItemAt(i))
		}
		return items
	}

	private inner class LyricAdapter(
		private val lyricList: MutableList<MediaStoreUtils.Lyric>
	) : MyRecyclerView.Adapter<LyricAdapter.ViewHolder>() {

		private var defaultTextColor = 0
		private var contrastTextColor = 0
		private var highlightTextColor = 0
		var currentFocusPos = -1
		private var currentTranslationPos = -1
		private var isBoldEnabled = false
		private var isLyricCentered = false
		private var isLyricContrastEnhanced = false

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
					ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
					val instance = activity.getPlayer()
					if (instance?.isPlaying == false) {
						instance.play()
					}
					instance?.seekTo(lyric.timeStamp)
				}
			}

			with(holder.lyricTextView) {
				visibility = if (lyric.content.isNotEmpty()) View.VISIBLE else View.GONE
				text = lyric.content

				if (isBoldEnabled) {
					this.typeface = TypefaceCompat.create(context,null, 700, false)
				} else {
					this.typeface = TypefaceCompat.create(context, null, 500, false)
				}

				if (isLyricCentered) {
					this.gravity = Gravity.CENTER
				} else {
					this.gravity = Gravity.START
				}

				val textSize = if (lyric.isTranslation) 20f else 28f
				val paddingTop = (if (lyric.isTranslation) 2 else 18).dpToPx(context)
				val paddingBottom = (if (position + 1 < lyricList.size &&
					lyricList[position + 1].isTranslation
				) 2 else 18).dpToPx(context)

				this.textSize = textSize
				setPadding(10.dpToPx(context), paddingTop, 10.dpToPx(context), paddingBottom)

				val isFocus = position == currentFocusPos || position == currentTranslationPos
				setTextColor(if (isFocus) highlightTextColor else (if (isLyricContrastEnhanced)
					contrastTextColor else defaultTextColor))
			}
		}

		override fun onAttachedToRecyclerView(recyclerView: MyRecyclerView) {
			super.onAttachedToRecyclerView(recyclerView)
			updateLyricStatus()
		}

		fun updateLyricStatus() {
			isBoldEnabled = prefs.getBooleanStrict("lyric_bold", false)
			isLyricCentered = prefs.getBooleanStrict("lyric_center", false)
			isLyricContrastEnhanced = prefs.getBooleanStrict("lyric_contrast", false)
		}

		override fun getItemCount(): Int = lyricList.size

		inner class ViewHolder(
			view: View
		) : RecyclerView.ViewHolder(view) {
			val lyricTextView: TextView = view.findViewById(R.id.lyric)
			val lyricCard: MaterialCardView = view.findViewById(R.id.cardview)
		}

		@SuppressLint("NotifyDataSetChanged")
		fun updateTextColor(newColorConstrast: Int, newColor: Int, newHighlightColor: Int) {
			defaultTextColor = newColor
			contrastTextColor = newColorConstrast
			highlightTextColor = newHighlightColor
			notifyDataSetChanged()
		}

		fun updateHighlight(position: Int) {
			if (currentFocusPos == position) return
			if (position >= 0) {
				currentFocusPos.let {
					notifyItemChanged(it)
					currentFocusPos = position
					notifyItemChanged(currentFocusPos)
				}

				if (position + 1 < lyricList.size &&
					lyricList[position + 1].isTranslation
				) {
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
				currentFocusPos = -1
				currentTranslationPos = -1
			}
		}
	}


	private class PlaylistCardAdapter(
		private val playlist: MutableList<MediaItem>,
		private val activity: MainActivity
	) : MyRecyclerView.Adapter<PlaylistCardAdapter.ViewHolder>() {

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
				ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
				val instance = activity.getPlayer()
				val pos = holder.bindingAdapterPosition
				playlist.removeAt(pos)
				notifyItemRemoved(pos)
				instance?.removeMediaItem(pos)
			}
			holder.itemView.setOnClickListener {
				ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
				val instance = activity.getPlayer()
				instance?.seekToDefaultPosition(holder.absoluteAdapterPosition)
			}
		}

		override fun onViewRecycled(holder: ViewHolder) {
			super.onViewRecycled(holder)
			Glide.with(activity.applicationContext).clear(holder.songCover)
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

	/*
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

	 */


	fun updateLyric(duration: Long?) {
		if (bottomSheetFullLyricList.isNotEmpty()) {
			val newIndex: Int

			val filteredList = bottomSheetFullLyricList.filterIndexed { _, lyric ->
				lyric.timeStamp <= (instance?.currentPosition ?: 0)
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
				newIndex != bottomSheetFullLyricAdapter.currentFocusPos
			) {
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
				return super.calculateDtToFit(
					viewStart,
					viewEnd,
					boxStart,
					boxEnd,
					snapPreference
				) + context.resources.displayMetrics.heightPixels / 3
			}

			override fun getVerticalSnapPreference(): Int {
				return SNAP_TO_START
			}

			override fun calculateTimeForDeceleration(dx: Int): Int {
				return 500
			}
		}

	private val positionRunnable = object : Runnable {
		override fun run() {
			if (!runnableRunning) return
			val position =
				CalculationUtils.convertDurationToTimeStamp(instance?.currentPosition ?: 0)
			val duration = instance?.currentMediaItem?.mediaMetadata?.extras?.getLong("Duration")
			if (duration != null && !isUserTracking) {
				bottomSheetFullSeekBar.max = duration.toInt()
				bottomSheetFullSeekBar.progress = instance?.currentPosition?.toInt() ?: 0
				bottomSheetFullSlider.valueTo = duration.toFloat()
				bottomSheetFullSlider.value =
					min(instance?.currentPosition?.toFloat() ?: 0f, bottomSheetFullSlider.valueTo)
				bottomSheetFullPosition.text = position
			}
			updateLyric(duration)
			if (instance?.isPlaying == true) {
				handler.postDelayed(this, SLIDER_UPDATE_INTERVAL)
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
		bottomSheetFullLyricAdapter.updateHighlight(0)
	}

}