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

package org.akanework.gramophone.logic

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.preference.PreferenceManager
import cn.lyric.getter.api.API
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.LastPlayedManager
import org.akanework.gramophone.logic.utils.LrcUtils.extractAndParseLyrics
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import kotlin.properties.Delegates


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

        const val SERVICE_SET_TIMER = "set_timer"
        const val SERVICE_QUERY_TIMER = "query_timer"
        const val SERVICE_GET_LYRICS = "get_lyrics"
        const val SERVICE_TIMER_CHANGED = "changed_timer"

        /** Flyme 状态栏歌词 TICKER 一直显示 */
        private const val FLAG_ALWAYS_SHOW_TICKER = 0x1000000

        /** 只更新 Flyme 状态栏歌词，不更新封面等 */
        private const val FLAG_ONLY_UPDATE_TICKER = 0x2000000

    }

    private val positionRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                if (lyrics != null) {
                    val player1 = this@GramophonePlaybackService.mediaSession!!.player.currentPosition
                    nowLyric = lyrics!!.last { it.timeStamp <= player1 }.content
                    handler.postDelayed(this, 300)
                }
            }
        }
    }
    private var mediaSession: MediaLibrarySession? = null
    private var lyrics: List<MediaStoreUtils.Lyric>? = null
    private lateinit var customCommands: List<CommandButton>
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private lateinit var lastPlayedManager: LastPlayedManager
    private val notification: Notification by lazy {
        NotificationCompat.Builder(this@GramophonePlaybackService, "YOUR_NOTIFICATION_CHANNEL_ID").apply {
            setSmallIcon(R.drawable.ic_gramophone)
            extras.putBoolean("ticker_icon_switch", false)
        }.build().apply { flags = FLAG_ALWAYS_SHOW_TICKER or FLAG_ONLY_UPDATE_TICKER }
    }
    private val lga: API by lazy { API() }
    private val manager: NotificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    var nowLyric: String by Delegates.observable("") { _, old, new ->
        if (old == new || new.isEmpty()) return@observable
        Log.d("Lyrics", new)
//        模块，暂停歌词不用管
        lga.sendLyric(new)
//        flyme
        notification.apply {
            tickerText = new
            manager.notify(114514, this)
            //        在暂停的地方加个manager.cancelAll()就可以清楚歌词
        }
    }


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
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        customCommands =
            listOf(
                CommandButton.Builder() // shuffle currently disabled, click will enable
                    .setDisplayName(getString(R.string.shuffle))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_SHUFFLE_ACTION_ON, Bundle.EMPTY)
                    )
                    .setIconResId(R.drawable.ic_shuffle)
                    .build(),
                CommandButton.Builder() // shuffle currently enabled, click will disable
                    .setDisplayName(getString(R.string.shuffle))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_SHUFFLE_ACTION_OFF, Bundle.EMPTY)
                    )
                    .setIconResId(R.drawable.ic_shuffle_on)
                    .build(),
                CommandButton.Builder() // repeat currently disabled, click will repeat all
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_REPEAT_ALL, Bundle.EMPTY)
                    )
                    .setIconResId(R.drawable.ic_repeat)
                    .build(),
                CommandButton.Builder() // repeat all currently enabled, click will repeat one
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_REPEAT_ONE, Bundle.EMPTY)
                    )
                    .setIconResId(R.drawable.ic_repeat_on)
                    .build(),
                CommandButton.Builder() // repeat one currently enabled, click will disable
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setSessionCommand(
                        SessionCommand(PLAYBACK_REPEAT_OFF, Bundle.EMPTY)
                    )
                    .setIconResId(R.drawable.ic_repeat_one_on)
                    .build(),
            )

        val player = ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this)
                .setEnableAudioFloatOutput(prefs.getBoolean("floatoutput", false))
                .setEnableDecoderFallback(true)
                .setEnableAudioTrackPlaybackParams(
                    prefs.getBoolean(
                        "ps_hardware_acc",
                        true
                    )
                ) // hardware/system-accelerated playback speed
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSkipSilenceEnabled(prefs.getBoolean("skip_silence", false))
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
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
            try {
                val restoreInstance = lastPlayedManager.restore()
                if (restoreInstance != null) {
                    player.setMediaItems(
                        restoreInstance.mediaItems,
                        restoreInstance.startIndex, restoreInstance.startPositionMs
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
            putExtra(
                AudioEffect.EXTRA_AUDIO_SESSION,
                (mediaSession!!.player as ExoPlayer).audioSessionId
            )
        })
        mediaSession!!.player.release()
        mediaSession!!.release()
        mediaSession = null
        lyrics = null
        super.onDestroy()
    }

    // This onGetSession is a necessary method override needed by
    // MediaSessionService.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

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
        availableSessionCommands.add(SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY))
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

            SERVICE_GET_LYRICS -> {
                SessionResult(SessionResult.RESULT_SUCCESS).also {
                    it.extras.putParcelableArray("lyrics", lyrics?.toTypedArray())
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

    override fun onTracksChanged(tracks: Tracks) {
        lyrics = null
        for (i in tracks.groups) {
            for (j in 0 until i.length) {
                val trackMetadata = i.getTrackFormat(j).metadata ?: continue
                lyrics = extractAndParseLyrics(
                    mediaSession?.player?.currentMediaItem?.getFile(), trackMetadata
                ) ?: continue
                handler.postDelayed(positionRunnable, 0)
            }
        }
        mediaSession!!.broadcastCustomCommand(
            SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lyrics = null
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