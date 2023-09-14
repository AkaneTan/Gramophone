package org.akanework.gramophone.ui.fragments

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.akanework.gramophone.Constants
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.services.GramophonePlaybackService
import org.akanework.gramophone.logic.utils.GramophoneUtils
import org.akanework.gramophone.logic.utils.MyBottomSheetBehavior
import org.akanework.gramophone.logic.utils.playOrPause
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

open class PlayerFragment : BaseFragment(), Player.Listener {

	private val handler = Handler(Looper.getMainLooper())
	private val libraryViewModel: LibraryViewModel by activityViewModels()

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
	private lateinit var bottomSheetFullSlideUpButton: MaterialButton
	private lateinit var bottomSheetShuffleButton: MaterialButton
	private lateinit var bottomSheetLoopButton: MaterialButton
	private lateinit var bottomSheetPlaylistButton: MaterialButton
	private lateinit var bottomSheetLyricButton: MaterialButton
	private lateinit var bottomSheetTimerButton: MaterialButton
	private lateinit var bottomSheetFullSlider: Slider

	private lateinit var standardBottomSheet: FrameLayout
	private lateinit var standardBottomSheetBehavior: MyBottomSheetBehavior<FrameLayout>

	private var isUserTracking = false
	private var runnableRunning = false
	private var waitedForContainer = true

	private lateinit var sessionToken: SessionToken
	private lateinit var controllerFuture: ListenableFuture<MediaController>

	private val positionRunnable = object : Runnable {
		override fun run() {
			val instance = controllerFuture.get()!!
			val position =
				GramophoneUtils.convertDurationToTimeStamp(instance.currentPosition)
			if (runnableRunning) {
				val duration =
					libraryViewModel.durationItemList.value?.get(
						instance.currentMediaItem?.mediaId?.toLong(),
					)
				if (duration != null && !isUserTracking) {
					bottomSheetFullSlider.value =
						(instance.currentPosition.toFloat() / duration.toFloat()).coerceAtMost(1f)
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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// Case 1: We got started by another PlayerFragment
		waitForContainer = arguments?.getBoolean("WaitForContainer") ?: false
		if (waitForContainer) {
			waitedForContainer = false
		}
	}

	override fun onStart() {
		// Case 2: We start another player fragment, and get restarted in back stack after user is done
		// Note: keep this line before super.onStart() to avoid value being overwritten in BaseFragment
		if ((requireActivity() as MainActivity).waitForContainer) {
			waitedForContainer = false
		}
		super.onStart()
		sessionToken =
			SessionToken(requireContext(), ComponentName(requireContext(), GramophonePlaybackService::class.java))
		controllerFuture =
			MediaController
				.Builder(requireContext(), sessionToken)
				.setListener(sessionListener)
				.buildAsync()
		controllerFuture.addListener(
			{
				val controller = controllerFuture.get()
				controller.addListener(this)
				bottomSheetTimerButton.isChecked = alreadyHasTimer(controller)
				onRepeatModeChanged(controller.repeatMode)
				onShuffleModeEnabledChanged(controller.shuffleModeEnabled)
				onIsPlayingChanged(controller.isPlaying)
				updateSongInfo(controller.currentMediaItem)
				handler.post { startPostponedEnterTransition() }
			},
			MoreExecutors.directExecutor(),
		)
	}

	override fun onStop() {
		super.onStop()
		val instance = controllerFuture.get()
		instance.removeListener(this)
		controllerFuture.get().release()
	}

	fun getPlayer(): MediaController = controllerFuture.get()

	override fun onMediaItemTransition(
		mediaItem: MediaItem?,
		reason: Int,
	) {
		updateSongInfo(mediaItem)
	}

	private fun queryTimerDuration(controller: MediaController) : Int =
		controller.sendCustomCommand(
			SessionCommand(Constants.SERVICE_QUERY_TIMER, Bundle.EMPTY),
			Bundle.EMPTY).get().extras.getInt("duration")

	private fun alreadyHasTimer(controller: MediaController) : Boolean =
		queryTimerDuration(controller) > 0

	private fun setTimer(controller: MediaController, value: Int) =
		controller.sendCustomCommand(
			SessionCommand(Constants.SERVICE_SET_TIMER, Bundle.EMPTY).apply {
				customExtras.putInt("duration", value)
			}, Bundle.EMPTY)

	private val sessionListener: MediaController.Listener = object : MediaController.Listener {
		override fun onCustomCommand(
			controller: MediaController,
			command: SessionCommand,
			args: Bundle
		): ListenableFuture<SessionResult> {
			if (command.customAction == Constants.SERVICE_TIMER_CHANGED) {
				bottomSheetTimerButton.isChecked = alreadyHasTimer(controller)
			}
			return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
		}
	}

	private fun updateSongInfo(mediaItem: MediaItem?) {
		val instance = controllerFuture.get()
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
			bottomSheetPreviewSubtitle.text = mediaItem?.mediaMetadata?.artist
			bottomSheetFullTitle.text = mediaItem?.mediaMetadata?.title
			bottomSheetFullSubtitle.text = mediaItem?.mediaMetadata?.artist
			bottomSheetFullDuration.text =
				mediaItem
					?.mediaId
					?.let { libraryViewModel.durationItemList.value?.get(it.toLong()) }
					?.let { GramophoneUtils.convertDurationToTimeStamp(it) }
		}
		var newState = standardBottomSheetBehavior.state
		if (instance.mediaItemCount != 0) {
			if (newState != BottomSheetBehavior.STATE_EXPANDED) {
				newState = BottomSheetBehavior.STATE_COLLAPSED
			}
		} else {
			newState = BottomSheetBehavior.STATE_HIDDEN
		}
		handler.post {
			if (!waitedForContainer) {
				standardBottomSheetBehavior.setStateWithoutAnimation(newState)
				waitedForContainer = true
			} else {
				standardBottomSheetBehavior.state = newState
			}
		}
	}

	// When the slider is dragged by user, mark it
	// to use this state later.
	private val touchListener: Slider.OnSliderTouchListener =
		object : Slider.OnSliderTouchListener {
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


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (waitForContainer) {
			postponeEnterTransition()
		}

		// Initialize layouts.
		standardBottomSheet = view.findViewById(R.id.player_layout)
		standardBottomSheetBehavior = MyBottomSheetBehavior.from(standardBottomSheet)

		bottomSheetPreviewCover = view.findViewById(R.id.preview_album_cover)
		bottomSheetPreviewTitle = view.findViewById(R.id.preview_song_name)
		bottomSheetPreviewSubtitle = view.findViewById(R.id.preview_artist_name)
		bottomSheetPreviewControllerButton = view.findViewById(R.id.preview_control)
		bottomSheetPreviewNextButton = view.findViewById(R.id.preview_next)

		bottomSheetFullCover = view.findViewById(R.id.full_sheet_cover)
		bottomSheetFullTitle = view.findViewById(R.id.full_song_name)
		bottomSheetFullSubtitle = view.findViewById(R.id.full_song_artist)
		bottomSheetFullPreviousButton = view.findViewById(R.id.sheet_previous_song)
		bottomSheetFullControllerButton = view.findViewById(R.id.sheet_mid_button)
		bottomSheetFullNextButton = view.findViewById(R.id.sheet_next_song)
		bottomSheetFullPosition = view.findViewById(R.id.position)
		bottomSheetFullDuration = view.findViewById(R.id.duration)
		bottomSheetFullSlider = view.findViewById(R.id.slider)
		bottomSheetFullSlideUpButton = view.findViewById(R.id.slide_down)
		bottomSheetShuffleButton = view.findViewById(R.id.sheet_random)
		bottomSheetLoopButton = view.findViewById(R.id.sheet_loop)
		bottomSheetLyricButton = view.findViewById(R.id.lyrics)
		bottomSheetTimerButton = view.findViewById(R.id.timer)
		bottomSheetPlaylistButton = view.findViewById(R.id.playlist)

		val previewPlayer = view.findViewById<RelativeLayout>(R.id.preview_player)
		val fullPlayer = view.findViewById<RelativeLayout>(R.id.full_player)

		standardBottomSheet.setOnClickListener {
			if (standardBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
				standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
			}
		}

		bottomSheetTimerButton.setOnClickListener {
			val controller = controllerFuture.get()
			val picker =
				MaterialTimePicker
					.Builder()
					.setHour(queryTimerDuration(controller) / 3600 / 1000)
					.setMinute((queryTimerDuration(controller) % (3600 * 1000)) / (60 * 1000))
					.setTimeFormat(TimeFormat.CLOCK_24H)
					.setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
					.build()
			picker.addOnPositiveButtonClickListener {
				val destinationTime: Int = picker.hour * 1000 * 3600 + picker.minute * 1000 * 60
				setTimer(controllerFuture.get(), destinationTime)
			}
			picker.addOnDismissListener {
				bottomSheetTimerButton.isChecked = alreadyHasTimer(controllerFuture.get())
			}
			picker.show(requireActivity().supportFragmentManager, "timer")
		}

		bottomSheetLoopButton.setOnClickListener {
			val instance = controllerFuture.get()
			instance.repeatMode = when (instance.repeatMode) {
				Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
				Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
				Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
				else -> throw IllegalStateException()
			}
		}

		bottomSheetPreviewControllerButton.setOnClickListener {
			controllerFuture.get().playOrPause()
		}
		bottomSheetFullControllerButton.setOnClickListener {
			controllerFuture.get().playOrPause()
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
		bottomSheetShuffleButton.addOnCheckedChangeListener { _, isChecked ->
			controllerFuture.get().shuffleModeEnabled = isChecked
		}

		bottomSheetFullSlider.addOnChangeListener { _, value, isUser ->
			if (isUser) {
				val instance = controllerFuture.get()
				val dest =
					instance.currentMediaItem?.mediaId?.let {
						libraryViewModel.durationItemList.value?.get(it.toLong())
					}
				if (dest != null) {
					bottomSheetFullPosition.text =
						GramophoneUtils.convertDurationToTimeStamp((value * dest).toLong())
				}
			}
		}

		bottomSheetFullSlider.addOnSliderTouchListener(touchListener)

		bottomSheetFullSlideUpButton.setOnClickListener {
			standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
		}

		val bottomSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
			override fun handleOnBackStarted(backEvent: BackEventCompat) {
				standardBottomSheetBehavior.startBackProgress(backEvent)
			}

			override fun handleOnBackProgressed(backEvent: BackEventCompat) {
				standardBottomSheetBehavior.updateBackProgress(backEvent)
			}

			override fun handleOnBackPressed() {
				standardBottomSheetBehavior.handleBackInvoked()
			}

			override fun handleOnBackCancelled() {
				standardBottomSheetBehavior.cancelBackProgress()
			}
		}
		requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, bottomSheetBackCallback)

		standardBottomSheetBehavior.addBottomSheetCallback(
			object : BottomSheetBehavior.BottomSheetCallback() {
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
							bottomSheetBackCallback.isEnabled = false
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
							bottomSheetBackCallback.isEnabled = true
						}

						BottomSheetBehavior.STATE_HIDDEN -> {
							previewPlayer.visibility = View.GONE
							fullPlayer.visibility = View.GONE
							previewPlayer.alpha = 0f
							fullPlayer.alpha = 0f
							bottomSheetBackCallback.isEnabled = false
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
		)
		standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
	}

	override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
		bottomSheetShuffleButton.isChecked = shuffleModeEnabled
	}

	override fun onRepeatModeChanged(repeatMode: Int) {
		when (repeatMode) {
			Player.REPEAT_MODE_ALL -> {
				bottomSheetLoopButton.isChecked = true
				bottomSheetLoopButton.icon =
					AppCompatResources.getDrawable(requireContext(), R.drawable.ic_repeat)
			}

			Player.REPEAT_MODE_ONE -> {
				bottomSheetLoopButton.isChecked = true
				bottomSheetLoopButton.icon =
					AppCompatResources.getDrawable(requireContext(), R.drawable.ic_repeat_one)
			}

			Player.REPEAT_MODE_OFF -> {
				bottomSheetLoopButton.isChecked = false
				bottomSheetLoopButton.icon =
					AppCompatResources.getDrawable(requireContext(), R.drawable.ic_repeat)
			}
		}
	}

	override fun onIsPlayingChanged(isPlaying: Boolean) {
		val instance = controllerFuture.get()
		if (isPlaying) {
			bottomSheetPreviewControllerButton.icon =
				AppCompatResources.getDrawable(requireContext(), R.drawable.pause_art)
			bottomSheetFullControllerButton.icon =
				AppCompatResources.getDrawable(requireContext(), R.drawable.pause_art)
		} else if (instance.playbackState != Player.STATE_BUFFERING) {
			bottomSheetPreviewControllerButton.icon =
				AppCompatResources.getDrawable(requireContext(), R.drawable.play_art)
			bottomSheetFullControllerButton.icon =
				AppCompatResources.getDrawable(requireContext(), R.drawable.play_art)
		}
		if (isPlaying) {
			if (!runnableRunning) {
				handler.postDelayed(positionRunnable, instance.currentPosition % 1000)
				runnableRunning = true
			}
		}
	}

}