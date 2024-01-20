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

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.GramophonePlaybackService
import org.akanework.gramophone.logic.fadInAnimation
import org.akanework.gramophone.logic.fadOutAnimation
import org.akanework.gramophone.logic.playOrPause
import org.akanework.gramophone.logic.startAnimation
import org.akanework.gramophone.logic.ui.MyBottomSheetBehavior
import org.akanework.gramophone.ui.MainActivity


class PlayerBottomSheet private constructor(
    context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes),
    Player.Listener, DefaultLifecycleObserver {
    constructor(context: Context, attributeSet: AttributeSet?)
            : this(context, attributeSet, 0, 0)

    private var sessionToken: SessionToken? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var standardBottomSheetBehavior: MyBottomSheetBehavior<FrameLayout>? = null
    private var bottomSheetBackCallback: OnBackPressedCallback? = null
    val fullPlayer: FullBottomSheet
    private val previewPlayer: View
    private val bottomSheetPreviewCover: ImageView
    private val bottomSheetPreviewTitle: TextView
    private val bottomSheetPreviewSubtitle: TextView
    private val bottomSheetPreviewControllerButton: MaterialButton
    private val bottomSheetPreviewNextButton: MaterialButton

    private val activity
        get() = context as MainActivity
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val lifecycleOwner: LifecycleOwner
        get() = activity
    private val handler = Handler(Looper.getMainLooper())
    private val instance: MediaController
        get() = controllerFuture!!.get()
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
                    if (controllerFuture?.isDone == true && controllerFuture?.isCancelled == false
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
        inflate(context, R.layout.bottom_sheet_impl, this)
        id = R.id.player_layout
        previewPlayer = findViewById(R.id.preview_player)
        fullPlayer = findViewById(R.id.full_player)
        fullPlayer.init()
        bottomSheetPreviewTitle = findViewById(R.id.preview_song_name)
        bottomSheetPreviewSubtitle = findViewById(R.id.preview_artist_name)
        bottomSheetPreviewCover = findViewById(R.id.preview_album_cover)
        bottomSheetPreviewControllerButton = findViewById(R.id.preview_control)
        bottomSheetPreviewNextButton = findViewById(R.id.preview_next)

        ViewCompat.setOnApplyWindowInsetsListener(previewPlayer) { view, insets ->
            view.onApplyWindowInsets(insets.toWindowInsets())
            doOnLayout {
                val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val notchInset = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                previewPlayer.setPadding(notchInset.left, 0, notchInset.right,
                    notchInset.bottom + navBarInset.bottom)
                fullPlayer.setPadding(notchInset.left, statusBarInset.top,
                    notchInset.right, notchInset.bottom + navBarInset.bottom)
                previewPlayer.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.UNSPECIFIED
                )
                standardBottomSheetBehavior?.setPeekHeight(previewPlayer.measuredHeight, false)
            }
            return@setOnApplyWindowInsetsListener insets
        }

        setOnClickListener {
            if (standardBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetPreviewControllerButton.setOnClickListener {
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

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        post {
            standardBottomSheetBehavior = MyBottomSheetBehavior.from(this)
            fullPlayer.minimize = {
                standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED }
            standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    if (fullPlayer.bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE
                    ) {
                        fullPlayer.bottomSheetFullLyricRecyclerView.fadOutAnimation(FullBottomSheet.LYRIC_FADE_TRANSITION_SEC)
                        fullPlayer.bottomSheetLyricButton.isChecked = false
                    } else {
                        standardBottomSheetBehavior!!.startBackProgress(backEvent)
                    }
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    if (fullPlayer.bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE
                    ) {
                        // TODO
                    } else {
                        standardBottomSheetBehavior!!.updateBackProgress(backEvent)
                    }
                }

                override fun handleOnBackPressed() {
                    if (fullPlayer.bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE
                    ) {
                        fullPlayer.bottomSheetFullLyricRecyclerView.fadOutAnimation(FullBottomSheet.LYRIC_FADE_TRANSITION_SEC)
                        fullPlayer.bottomSheetLyricButton.isChecked = false
                    } else {
                        standardBottomSheetBehavior!!.handleBackInvoked()
                    }
                }

                override fun handleOnBackCancelled() {
                    if (fullPlayer.bottomSheetFullLyricRecyclerView.visibility ==
                        View.VISIBLE
                    ) {
                        fullPlayer.bottomSheetFullLyricRecyclerView.fadInAnimation(FullBottomSheet.LYRIC_FADE_TRANSITION_SEC)
                        fullPlayer.bottomSheetLyricButton.isChecked = false
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
        fullPlayer.minimize = {}
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
                .with(context)
                .load(mediaItem?.mediaMetadata?.artworkUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_default_cover)
                .into(bottomSheetPreviewCover)
            bottomSheetPreviewTitle.text = mediaItem?.mediaMetadata?.title
            bottomSheetPreviewSubtitle.text =
                mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist)
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
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        onPlaybackStateChanged(instance.playbackState)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (instance.isPlaying) {
            if (bottomSheetPreviewControllerButton.getTag(R.id.play_next) as Int? != 1) {
                bottomSheetPreviewControllerButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.play_anim)
                bottomSheetPreviewControllerButton.icon.startAnimation()
                bottomSheetPreviewControllerButton.setTag(R.id.play_next, 1)
            }
        } else if (playbackState != Player.STATE_BUFFERING) {
            if (bottomSheetPreviewControllerButton.getTag(R.id.play_next) as Int? != 2) {
                bottomSheetPreviewControllerButton.icon =
                    AppCompatResources.getDrawable(context, R.drawable.pause_anim)
                bottomSheetPreviewControllerButton.icon.startAnimation()
                bottomSheetPreviewControllerButton.setTag(R.id.play_next, 2)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        sessionToken =
            SessionToken(context, ComponentName(context, GramophonePlaybackService::class.java))
        controllerFuture =
            MediaController
                .Builder(context, sessionToken!!)
                .setListener(fullPlayer.sessionListener)
                .buildAsync()
        controllerFuture!!.addListener(
            {
                instance.addListener(this)
                onPlaybackStateChanged(instance.playbackState)
                onMediaItemTransition(
                    instance.currentMediaItem,
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                )
                if (prefs.getBoolean("autoplay", false) && !instance.isPlaying) {
                    instance.play()
                }
                handler.post {
                    ready = true
                }
            },
            MoreExecutors.directExecutor(),
        )
        fullPlayer.onStart(controllerFuture!!)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        fullPlayer.onStop()
        if (controllerFuture?.isDone == true && controllerFuture?.isCancelled == false) {
            instance.removeListener(this)
            instance.release()
        }
        controllerFuture?.cancel(true)
    }

}