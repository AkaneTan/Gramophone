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
import org.akanework.gramophone.ui.components.PlayerBottomSheet
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

open class PlayerFragment : BaseFragment() {

	fun getPlayer(): MediaController =
		requireView().findViewById<PlayerBottomSheet>(R.id.player_layout).getPlayer()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// Case 1: We got started by another PlayerFragment
		waitForContainer = arguments?.getBoolean("WaitForContainer") ?: false
	}

	override fun onStart() {
		// Case 2: We start another player fragment, and get restarted in back stack after user is done
		// Note: keep this line before super.onStart() to avoid value being overwritten in BaseFragment
		if ((requireActivity() as MainActivity).waitForContainer) {
			requireView().findViewById<PlayerBottomSheet>(R.id.player_layout).waitedForContainer = false
		}
		super.onStart()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val playerLayout = view.findViewById<PlayerBottomSheet>(R.id.player_layout)
		playerLayout.onUiReadyListener = Runnable { startPostponedEnterTransition() }
		if (waitForContainer) {
			playerLayout.waitedForContainer = false
			postponeEnterTransition()
		}
	}
}