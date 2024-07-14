package org.akanework.gramophone.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.slider.Slider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.CalculationUtils.convertDurationToTimeStamp

class AudioPreviewActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var titleAndButtonsContainer: LinearLayout
    private lateinit var audioTitle: TextView
    private lateinit var artistTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var albumArt: ImageView
    private lateinit var timeSlider: Slider
    private lateinit var playPauseButton: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private val updateSliderRunnable = object : Runnable {
        override fun run() {
            timeSlider.value = player.currentPosition.toFloat()
            handler.postDelayed(this, 1000)
        }
    }

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_preview)

        audioTitle = findViewById(R.id.title_text_view)
        artistTextView = findViewById(R.id.artist_text_view)
        durationTextView = findViewById(R.id.duration_text_view)
        albumArt = findViewById(R.id.album_art)
        timeSlider = findViewById(R.id.time_slider)
        playPauseButton = findViewById(R.id.play_pause_replay_button)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        player.pause()
                        updatePlayPauseButton()
                    }
                }
            }
            .build()

        player = ExoPlayer.Builder(this).build()

        handleIntent(intent)

        playPauseButton.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (requestAudioFocus()) {
                    player.play()
                }
            }
            updatePlayPauseButton()
        }

        timeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                player.seekTo(value.toLong())
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        timeSlider.valueTo = player.duration.toFloat()
                        handler.post(updateSliderRunnable)
                    }

                    Player.STATE_ENDED -> finish()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                timeSlider.value = player.currentPosition.toFloat()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                audioTitle.text =
                    mediaMetadata.title ?: intent.data?.lastPathSegment ?: "Audio Title"
                artistTextView.text = mediaMetadata.artist ?: "Unknown Artist"
                durationTextView.text = convertDurationToTimeStamp(player.duration)

                mediaMetadata.artworkData?.let {
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    albumArt.setImageBitmap(bitmap)
                } ?: run {
                    albumArt.setImageResource(R.drawable.ic_default_cover)
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.play()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onResume() {
        super.onResume()
        if (requestAudioFocus()) {
            player.playWhenReady = true
            handler.post(updateSliderRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        player.playWhenReady = false
        handler.removeCallbacks(updateSliderRunnable)
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        handler.removeCallbacks(updateSliderRunnable)
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private fun updatePlayPauseButton() {
        playPauseButton.setImageResource(
            if (player.isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow
        )
    }
}
