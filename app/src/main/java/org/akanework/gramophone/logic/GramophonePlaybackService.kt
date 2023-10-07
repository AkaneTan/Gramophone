package org.akanework.gramophone.logic

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.LastPlayedManager


/**
 * [GramophonePlaybackService] is a server service.
 * It's using exoplayer2 as its player backend.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class GramophonePlaybackService : MediaLibraryService(),
    MediaLibraryService.MediaLibrarySession.Callback, Player.Listener {

    companion object {
        private const val PLAYBACK_SHUFFLE_ACTION_ON = "shuffle_on"
        private const val PLAYBACK_SHUFFLE_ACTION_OFF = "shuffle_off"
        private const val PLAYBACK_REPEAT_OFF = "repeat_off"
        private const val PLAYBACK_REPEAT_ALL = "repeat_all"
        private const val PLAYBACK_REPEAT_ONE = "repeat_one"
        // sent to PlayerBottomSheet
        const val SERVICE_TIMER_CHANGED = "changed_timer"
    }

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var customCommands: List<CommandButton>
    private lateinit var handler: Handler
    private lateinit var lastPlayedManager: LastPlayedManager

    private fun getRepeatCommand() =
        when (mediaSession?.player!!.repeatMode) {
            Player.REPEAT_MODE_OFF -> customCommands[2]
            Player.REPEAT_MODE_ALL -> customCommands[3]
            Player.REPEAT_MODE_ONE -> customCommands[4]
            else -> throw IllegalArgumentException()
        }

    private fun getShufflingCommand() =
        if (mediaSession?.player!!.shuffleModeEnabled)
            customCommands[1]
        else
            customCommands[0]

    private val timer: Runnable = Runnable {
        mediaSession?.player?.pause()
        timerDuration = 0
    }

    private var timerDuration = 0
        set(value) {
            field = value
            if (value > 0) {
                handler.postDelayed(timer, value.toLong())
            } else {
                handler.removeCallbacks(timer)
            }
            mediaSession!!.broadcastCustomCommand(
                SessionCommand(SERVICE_TIMER_CHANGED, Bundle.EMPTY),
                Bundle.EMPTY
            )
        }

    override fun onCreate() {
        customCommands =
            listOf(
                CommandButton.Builder() // shuffle currently disabled, click will enable
                    .setDisplayName(getString(R.string.shuffle))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_SHUFFLE_ACTION_ON, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_shuffle)
                    .build(),
                CommandButton.Builder() // shuffle currently enabled, click will disable
                    .setDisplayName(getString(R.string.shuffle))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_SHUFFLE_ACTION_OFF, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_shuffle_on)
                    .build(),
                CommandButton.Builder() // repeat currently disabled, click will repeat all
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_REPEAT_ALL, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_repeat)
                    .build(),
                CommandButton.Builder() // repeat all currently enabled, click will repeat one
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_REPEAT_ONE, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_repeat_on)
                    .build(),
                CommandButton.Builder() // repeat one currently enabled, click will disable
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_REPEAT_OFF, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_repeat_one_on)
                    .build(),
            )
        handler = Handler(Looper.getMainLooper())

        val player = ExoPlayer.Builder(this,
            DefaultRenderersFactory(this)
                .setEnableAudioFloatOutput(false) // TODO "true" disables EQ but enables HD audio, add toggle
                .setEnableDecoderFallback(true)
                .setEnableAudioTrackPlaybackParams(true) // hardware/system-accelerated playback speed
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            // we seek with SeekBar on a touchscreen anyway, we can afford loosing some exactness
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setSkipSilenceEnabled(false) // TODO add toggle?
            .setAudioAttributes(AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true)
            .build()
        sendBroadcast(Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        })

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(R.drawable.ic_gramophone)
        })
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, this)
                .setBitmapLoader(CacheBitmapLoader(DataSourceBitmapLoader(/* context= */ this)))
                .setSessionActivity(
                    getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
                    ),
                )
                .build()
        lastPlayedManager = LastPlayedManager(this, mediaSession!!)
        handler.post {
            val restoreInstance = lastPlayedManager.restore()
            if (restoreInstance != null) {
                player.setMediaItems(restoreInstance.mediaItems,
                    restoreInstance.startIndex, restoreInstance.startPositionMs)
            }
        }
        onShuffleModeEnabledChanged(mediaSession!!.player.shuffleModeEnabled)
        mediaSession!!.player.addListener(this)
        super.onCreate()
    }

    override fun onDestroy() {
        // When destroying, we should release server side player
        // alongside with the mediaSession.
        lastPlayedManager.save()
        sendBroadcast(Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                (mediaSession!!.player as ExoPlayer).audioSessionId)
        })
        mediaSession!!.player.release()
        mediaSession!!.release()
        mediaSession = null
        super.onDestroy()
    }

    // This onGetSession is a necessary method override needed by
    // MediaSessionService.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession?
            = mediaSession

    // Configure commands available to the controller in onConnect()
    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo)
            : MediaSession.ConnectionResult {
        val availableSessionCommands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        for (commandButton in customCommands) {
            // Add custom command to available session commands.
            commandButton.sessionCommand?.let { availableSessionCommands.add(it) }
        }
        availableSessionCommands.add(SessionCommand(SERVICE_SET_TIMER, Bundle.EMPTY))
        availableSessionCommands.add(SessionCommand(SERVICE_QUERY_TIMER, Bundle.EMPTY))
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableSessionCommands.build())
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return Futures.immediateFuture(when (customCommand.customAction) {
            PLAYBACK_SHUFFLE_ACTION_ON -> {
                session.player.shuffleModeEnabled = true
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            PLAYBACK_SHUFFLE_ACTION_OFF -> {
                session.player.shuffleModeEnabled = false
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            SERVICE_SET_TIMER -> {
                // 0 = clear timer
                timerDuration = customCommand.customExtras.getInt("duration")
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            SERVICE_QUERY_TIMER -> {
                SessionResult(SessionResult.RESULT_SUCCESS).also {
                    it.extras.putInt("duration", timerDuration)
                }
            }
            PLAYBACK_REPEAT_OFF -> {
                session.player.repeatMode = Player.REPEAT_MODE_OFF
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            PLAYBACK_REPEAT_ONE -> {
                session.player.repeatMode = Player.REPEAT_MODE_ONE
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            PLAYBACK_REPEAT_ALL -> {
                session.player.repeatMode = Player.REPEAT_MODE_ALL
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            else -> {
                SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
            }
        })
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val settable = SettableFuture.create<MediaItemsWithStartPosition>()
        handler.post {
            settable.set(lastPlayedManager.restore())
        }
        return settable
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lastPlayedManager.save()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        lastPlayedManager.save()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        mediaSession!!.setCustomLayout(ImmutableList.of(getRepeatCommand(), getShufflingCommand()))
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        mediaSession!!.setCustomLayout(ImmutableList.of(getRepeatCommand(), getShufflingCommand()))
    }

    // https://github.com/androidx/media/commit/6a5ac19140253e7e78ea65745914b0746e527058
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!mediaSession!!.player.playWhenReady) {
            stopSelf()
        }
    }
}

// Custom commands handling for client side

private const val SERVICE_SET_TIMER = "set_timer"
private const val SERVICE_QUERY_TIMER = "query_timer"

fun MediaController.getTimer(): Int =
    sendCustomCommand(
        SessionCommand(SERVICE_QUERY_TIMER, Bundle.EMPTY),
        Bundle.EMPTY
    ).get().extras.getInt("duration")
fun MediaController.hasTimer(): Boolean = getTimer() > 0
fun MediaController.setTimer(value: Int) {
    sendCustomCommand(
        SessionCommand(SERVICE_SET_TIMER, Bundle.EMPTY).apply {
            customExtras.putInt("duration", value)
        }, Bundle.EMPTY
    )
}