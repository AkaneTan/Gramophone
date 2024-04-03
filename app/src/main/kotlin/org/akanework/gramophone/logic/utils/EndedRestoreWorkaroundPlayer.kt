package org.akanework.gramophone.logic.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.PremiumActivity
import org.akanework.gramophone.logic.GramophoneApplication

/**
 * If player in STATE_ENDED is resumed, state will be STATE_READY, on play button press it will
 * update to STATE_ENDED and only then media3 will wrap around playlist for us. This is a workaround
 * to restore STATE_ENDED as well and fake it for media3 until it indeed wraps around playlist.
 */
@UnstableApi
class EndedRestoreWorkaroundPlayer(private val context: Context, player: Player)
	: ForwardingPlayer(player), Player.Listener {

	companion object {
		private const val TAG = "EndedRestore..Player"
	}

	val exoPlayer
		get() = wrappedPlayer as ExoPlayer
	var isEnded = false
		set(value) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "isEnded set to $value (was $field)")
			}
			field = value
			if (field) {
				wrappedPlayer.addListener(this)
			} else {
				wrappedPlayer.removeListener(this)
			}
		}

	override fun onPositionDiscontinuity(
		oldPosition: Player.PositionInfo,
		newPosition: Player.PositionInfo,
		reason: Int
	) {
		if (reason == DISCONTINUITY_REASON_SEEK) {
			isEnded = false
		}
		super.onPositionDiscontinuity(oldPosition, newPosition, reason)
	}

	override fun getAvailableCommands(): Player.Commands {
		val c = super.getAvailableCommands()
		if (!isPlaying || !(context.applicationContext as GramophoneApplication).autoPlay) return c
		return c.buildUpon()
			.remove(COMMAND_PLAY_PAUSE)
			.build()
	}

	override fun setPlayWhenReady(playWhenReady: Boolean) {
		super.setPlayWhenReady((context.applicationContext as GramophoneApplication).autoPlay || playWhenReady)
	}

	override fun pause() {
		if ((context.applicationContext as GramophoneApplication).autoPlay) tryPremium() else
		super.pause()
	}

	override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
		if (mediaItems.isNotEmpty() || !(context.applicationContext as GramophoneApplication).autoPlay) super.setMediaItems(mediaItems)
		else tryPremium()
	}

	override fun setMediaItems(
		mediaItems: MutableList<MediaItem>,
		startIndex: Int,
		startPositionMs: Long
	) {
		if (mediaItems.isNotEmpty() || !(context.applicationContext as GramophoneApplication).autoPlay) super.setMediaItems(mediaItems, startIndex, startPositionMs)
		else tryPremium()
	}

	override fun removeMediaItem(index: Int) {
		if (mediaItemCount > 1 || !(context.applicationContext as GramophoneApplication).autoPlay) super.removeMediaItem(index)
		else tryPremium()
	}

	fun tryPremium() {
		try {
			context.startActivity(Intent(context, PremiumActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		} catch (e: Exception) {}
	}

	override fun setRepeatMode(repeatMode: Int) {
		if (repeatMode != REPEAT_MODE_OFF || !(context.applicationContext as GramophoneApplication).autoPlay)super.setRepeatMode(repeatMode) else super.setRepeatMode(
			REPEAT_MODE_ALL)
	}

	override fun getPlaybackState(): Int {
		if (isEnded) return STATE_ENDED
		return super.getPlaybackState()
	}
}