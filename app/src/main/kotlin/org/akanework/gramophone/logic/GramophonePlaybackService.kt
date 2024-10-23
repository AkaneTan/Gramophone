/*
 *     Copyright (C) 2024 Akane Foundation
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

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.IllegalSeekPositionException
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.common.util.Util.isBitmapFactorySupportedMimeType
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.preference.PreferenceManager
import cn.xiaowine.xkt.AcTool.context
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.logic.utils.CircularShuffleOrder
import org.akanework.gramophone.logic.utils.LastPlayedManager
import org.akanework.gramophone.logic.utils.LrcUtils.LrcParserOptions
import org.akanework.gramophone.logic.utils.LrcUtils.extractAndParseLyrics
import org.akanework.gramophone.logic.utils.LrcUtils.extractAndParseLyricsLegacy
import org.akanework.gramophone.logic.utils.LrcUtils.loadAndParseLyricsFile
import org.akanework.gramophone.logic.utils.LrcUtils.loadAndParseLyricsFileLegacy
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.akanework.gramophone.logic.utils.exoplayer.EndedWorkaroundPlayer
import org.akanework.gramophone.logic.utils.exoplayer.GramophoneMediaSourceFactory
import org.akanework.gramophone.logic.utils.exoplayer.GramophoneRenderFactory
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.components.LegacyLyricsAdapter
import org.akanework.gramophone.ui.components.NewLyricsView
import kotlin.random.Random
import cn.lyric.getter.api.API
import cn.lyric.getter.api.data.ExtraData
import cn.lyric.getter.api.tools.Tools


/**
 * [GramophonePlaybackService] is a server service.
 * It's using exoplayer2 as its player backend.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class GramophonePlaybackService : MediaLibraryService(), MediaSessionService.Listener,
    MediaLibraryService.MediaLibrarySession.Callback, Player.Listener {

    companion object {
        private const val TAG = "GramoPlaybackService"
        private const val NOTIFY_CHANNEL_ID = "serviceFgsError"
        private const val NOTIFY_ID = 1
        private const val PENDING_INTENT_SESSION_ID = 0
        private const val PENDING_INTENT_NOTIFY_ID = 1
        private const val PLAYBACK_SHUFFLE_ACTION_ON = "shuffle_on"
        private const val PLAYBACK_SHUFFLE_ACTION_OFF = "shuffle_off"
        private const val PLAYBACK_REPEAT_OFF = "repeat_off"
        private const val PLAYBACK_REPEAT_ALL = "repeat_all"
        private const val PLAYBACK_REPEAT_ONE = "repeat_one"
        const val SERVICE_SET_TIMER = "set_timer"
        const val SERVICE_QUERY_TIMER = "query_timer"
        const val SERVICE_GET_LYRICS = "get_lyrics"
        const val SERVICE_GET_LYRICS_LEGACY = "get_lyrics_legacy"
        const val SERVICE_GET_SESSION = "get_session"
        const val SERVICE_TIMER_CHANGED = "changed_timer"
        private const val FLAG_ALWAYS_SHOW_TICKER = 0x01000000
        private const val FLAG_ONLY_UPDATE_TICKER = 0x02000000
    }
    private var newView: NewLyricsView? = null
    private var recyclerView: MyRecyclerView? = null
    private val adapter
        get() = recyclerView?.adapter as LegacyLyricsAdapter?
    private var lastSessionId = 0
    private var mediaSession: MediaLibrarySession? = null
    private var controller: MediaController? = null
    private var lyrics: SemanticLyrics? = null
    private var lyricsLegacy: MutableList<MediaStoreUtils.Lyric>? = null
    private var shuffleFactory:
            ((Int) -> ((CircularShuffleOrder) -> Unit) -> CircularShuffleOrder)? = null
    private lateinit var customCommands: List<CommandButton>
    private lateinit var handler: Handler
    private lateinit var nm: NotificationManagerCompat
    private lateinit var lastPlayedManager: LastPlayedManager
    private val lyricsLock = Semaphore(1)
    private lateinit var prefs: SharedPreferences
    private var highLyric : String = ""
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun getRepeatCommand() =
        when (controller!!.repeatMode) {
            Player.REPEAT_MODE_OFF -> customCommands[2]
            Player.REPEAT_MODE_ALL -> customCommands[3]
            Player.REPEAT_MODE_ONE -> customCommands[4]
            else -> throw IllegalArgumentException()
        }

    private fun getShufflingCommand() =
        if (controller!!.shuffleModeEnabled)
            customCommands[1]
        else
            customCommands[0]

    private val timer: Runnable = Runnable {
        controller!!.pause()
        timerDuration = null
    }

    private var timerDuration: Long? = null
        set(value) {
            field = value
            if (value != null && value > 0) {
                handler.postDelayed(timer, value - System.currentTimeMillis())
            } else {
                handler.removeCallbacks(timer)
            }
            mediaSession!!.broadcastCustomCommand(
                SessionCommand(SERVICE_TIMER_CHANGED, Bundle.EMPTY),
                Bundle.EMPTY
            )
        }

    private val headSetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                controller?.pause()
            }
        }
    }

    override fun onCreate() {
        handler = Handler(Looper.getMainLooper())
        super.onCreate()
        context = applicationContext
        nm = NotificationManagerCompat.from(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setListener(this)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_gramophone_monochrome)
            }
        )
        if (mayThrowForegroundServiceStartNotAllowed()) {
            // we don't need notification permission because this only is run on S/S_V2
            nm.createNotificationChannel(NotificationChannelCompat.Builder(
                NOTIFY_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH
            ).apply {
                setName(getString(R.string.fgs_failed_channel))
                setVibrationEnabled(true)
                setVibrationPattern(longArrayOf(0L, 200L))
                setLightsEnabled(false)
                setShowBadge(false)
                setSound(null, null)
            }.build()
            )
        } else if (nm.getNotificationChannel(NOTIFY_CHANNEL_ID) != null) {
            // for people who upgraded from S/S_V2 to newer version
            nm.deleteNotificationChannel(NOTIFY_CHANNEL_ID)
        }

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

        val player = EndedWorkaroundPlayer(
            ExoPlayer.Builder(
                this,
                GramophoneRenderFactory(this)
                    .setEnableAudioFloatOutput(
                        prefs.getBooleanStrict("floatoutput", false)
                    )
                    .setEnableDecoderFallback(true)
                    .setEnableAudioTrackPlaybackParams( // hardware/system-accelerated playback speed
                        prefs.getBooleanStrict("ps_hardware_acc", true)
                    )
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER),
                GramophoneMediaSourceFactory(this)
                /* .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING))
            TODO flag breaks playback of AcousticGuitar.mp3, report exo bug + add UI toggle*/
            )
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setSkipSilenceEnabled(prefs.getBooleanStrict("skip_silence", false))
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(), true
                )
                .build()
        )
        if (BuildConfig.DEBUG) {
            player.exoPlayer.addAnalyticsListener(EventLogger())
        }
        player.exoPlayer.audioSessionId = Util.generateAudioSessionIdV21(this)
        lastSessionId = player.exoPlayer.audioSessionId
        broadcastAudioSession()
        lastPlayedManager = LastPlayedManager(this, player)
        lastPlayedManager.allowSavingState = false

        mediaSession =
            MediaLibrarySession
                .Builder(this, player, this)
                .setBitmapLoader(object : BitmapLoader {
                    // Coil-based bitmap loader to reuse Coil's caching and to make sure we use
                    // the same cover art as the rest of the app, ie MediaStore's cover

                    override fun decodeBitmap(data: ByteArray) =
                        throw UnsupportedOperationException("decodeBitmap() not supported")

                    override fun loadBitmap(
                        uri: Uri
                    ): ListenableFuture<Bitmap> {
                        return CallbackToFutureAdapter.getFuture { completer ->
                            imageLoader.enqueue(
                                ImageRequest.Builder(this@GramophonePlaybackService)
                                    .data(uri)
                                    .allowHardware(false)
                                    .target(
                                        onStart = { _ ->
                                            // We don't need or want a placeholder.
                                        },
                                        onSuccess = { result ->
                                            completer.set((result as BitmapImage).bitmap)
                                        },
                                        onError = { _ ->
                                            completer.setException(
                                                Exception(
                                                    "coil onError called" +
                                                            " (normal if no album art exists)"
                                                )
                                            )
                                        }
                                    )
                                    .build())
                                .also {
                                    completer.addCancellationListener(
                                        { it.dispose() },
                                        ContextCompat.getMainExecutor(
                                            this@GramophonePlaybackService
                                        )
                                    )
                                }
                            "coil load for $uri"
                        }
                    }

                    override fun supportsMimeType(mimeType: String): Boolean {
                        return isBitmapFactorySupportedMimeType(mimeType)
                    }

                    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
                        return metadata.artworkUri?.let { loadBitmap(it) }
                    }
                })
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        PENDING_INTENT_SESSION_ID,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                )
                .build()
        controller = MediaController.Builder(this, mediaSession!!.token).buildAsync().get()
        handler.post {
            if (mediaSession == null) return@post
            lastPlayedManager.restore { items, factory ->
                if (mediaSession == null) return@restore
                applyShuffleSeed(true, factory.toFactory(controller!!))
                if (items != null) {
                    try {
                        mediaSession?.player?.setMediaItems(
                            items.mediaItems, items.startIndex, items.startPositionMs
                        )
                    } catch (e: IllegalSeekPositionException) {
                        Log.e(TAG, "failed to restore: " + Log.getStackTraceString(e))
                        // song was edited to be shorter and playback position doesn't exist anymore
                    }
                    // Prepare Player after UI thread is less busy (loads tracks, required for lyric)
                    handler.post {
                        controller?.prepare()
                    }
                }
                lastPlayedManager.allowSavingState = true
            }
        }
        onShuffleModeEnabledChanged(controller!!.shuffleModeEnabled) // refresh custom commands
        controller!!.addListener(this)
        registerReceiver(
            headSetReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
        startTimer()
    }

    // When destroying, we should release server side player
    // alongside with the mediaSession.
    override fun onDestroy() {
        // Important: this must happen before sending stop() as that changes state ENDED -> IDLE
        lastPlayedManager.save()
        mediaSession!!.player.stop()
        broadcastAudioSessionClose()
        controller!!.release()
        controller = null
        mediaSession!!.release()
        mediaSession!!.player.release()
        mediaSession = null
        lyrics = null
        unregisterReceiver(headSetReceiver)
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
        if (session.isMediaNotificationController(controller)
            || session.isAutoCompanionController(controller)
            || session.isAutomotiveController(controller)) {
            // currently, all custom actions are only useful when used by notification
            // other clients hopefully have repeat/shuffle buttons like MCT does
            for (commandButton in customCommands) {
                // Add custom command to available session commands.
                commandButton.sessionCommand?.let { availableSessionCommands.add(it) }
            }
        }
        availableSessionCommands.add(SessionCommand(SERVICE_SET_TIMER, Bundle.EMPTY))
        availableSessionCommands.add(SessionCommand(SERVICE_GET_SESSION, Bundle.EMPTY))
        availableSessionCommands.add(SessionCommand(SERVICE_QUERY_TIMER, Bundle.EMPTY))
        availableSessionCommands.add(SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY))
        availableSessionCommands.add(SessionCommand(SERVICE_GET_LYRICS_LEGACY, Bundle.EMPTY))
        handler.post {
            session.sendCustomCommand(
                controller,
                SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY),
                Bundle.EMPTY
            )
        }
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableSessionCommands.build())
            .build()
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
        broadcastAudioSessionClose()
        lastSessionId = audioSessionId
        broadcastAudioSession()
    }

    private fun broadcastAudioSession() {
        if (lastSessionId != 0) {
            sendBroadcast(Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, lastSessionId)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            })
        } else {
            Log.e(TAG, "session id is 0? why????? THIS MIGHT BREAK EQUALIZER")
        }
    }

    private fun broadcastAudioSessionClose() {
        if (lastSessionId != 0) {
            sendBroadcast(Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, lastSessionId)
            })
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return Futures.immediateFuture(when (customCommand.customAction) {
            PLAYBACK_SHUFFLE_ACTION_ON -> {
                this.controller!!.shuffleModeEnabled = true
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            PLAYBACK_SHUFFLE_ACTION_OFF -> {
                this.controller!!.shuffleModeEnabled = false
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            SERVICE_SET_TIMER -> {
                // 0 = clear timer
                customCommand.customExtras.getInt("duration").let {
                    timerDuration = if (it > 0) System.currentTimeMillis() + it else null
                }
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            SERVICE_QUERY_TIMER -> {
                SessionResult(SessionResult.RESULT_SUCCESS).also {
                    timerDuration?.let { td ->
                        it.extras.putInt("duration", (td - System.currentTimeMillis()).toInt())
                    }
                }
            }

            SERVICE_GET_LYRICS -> {
                SessionResult(SessionResult.RESULT_SUCCESS).also {
                    it.extras.putParcelable("lyrics", lyrics)
                }
            }

            SERVICE_GET_LYRICS_LEGACY -> {
                SessionResult(SessionResult.RESULT_SUCCESS).also {
                    it.extras.putParcelableArray("lyrics", lyricsLegacy?.toTypedArray())
                }
            }

            SERVICE_GET_SESSION -> {
                SessionResult(SessionResult.RESULT_SUCCESS).also {
                    it.extras.putInt("session", lastSessionId)
                }
            }

            PLAYBACK_REPEAT_OFF -> {
                this.controller!!.repeatMode = Player.REPEAT_MODE_OFF
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            PLAYBACK_REPEAT_ONE -> {
                this.controller!!.repeatMode = Player.REPEAT_MODE_ONE
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            PLAYBACK_REPEAT_ALL -> {
                this.controller!!.repeatMode = Player.REPEAT_MODE_ALL
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            else -> {
                SessionResult(SessionError.ERROR_BAD_VALUE)
            }
        })
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val settable = SettableFuture.create<MediaItemsWithStartPosition>()
        lastPlayedManager.restore { items, factory ->
            applyShuffleSeed(true, factory.toFactory(this.controller!!))
            if (items == null) {
                settable.setException(NullPointerException(
                    "null MediaItemsWithStartPosition, see former logs for root cause"))
            } else if (items.mediaItems.isNotEmpty()) {
                settable.set(items)
            } else {
                settable.setException(IndexOutOfBoundsException(
                    "LastPlayedManager restored empty MediaItemsWithStartPosition"))
            }
        }
        return settable
    }

    override fun onTracksChanged(tracks: Tracks) {
        val mediaItem = controller!!.currentMediaItem
        lyricsLock.runInBg {
            val trim = prefs.getBoolean("trim_lyrics", true)
            val multiLine = prefs.getBoolean("lyric_multiline", false)
            val newParser = prefs.getBoolean("lyric_parser", false)
            val options = LrcParserOptions(trim = trim, multiLine = multiLine,
                errorText = getString(androidx.media3.session.R.string.error_message_io))
            if (newParser) {
                var lrc = loadAndParseLyricsFile(mediaItem?.getFile(), options)
                if (lrc == null) {
                    loop@ for (i in tracks.groups) {
                        for (j in 0 until i.length) {
                            if (!i.isTrackSelected(j)) continue
                            // note: wav files can have null metadata
                            val trackMetadata = i.getTrackFormat(j).metadata ?: continue
                            lrc = extractAndParseLyrics(trackMetadata, options) ?: continue
                            break@loop
                        }
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    mediaSession?.let {
                        lyrics = lrc
                        lyricsLegacy = null
                        it.broadcastCustomCommand(
                            SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY),
                            Bundle.EMPTY
                        )
                    }
                }.join()
            } else {
                var lrc = loadAndParseLyricsFileLegacy(mediaItem?.getFile(), options)
                if (lrc == null) {
                    loop@ for (i in tracks.groups) {
                        for (j in 0 until i.length) {
                            if (!i.isTrackSelected(j)) continue
                            // note: wav files can have null metadata
                            val trackMetadata = i.getTrackFormat(j).metadata ?: continue
                            lrc = extractAndParseLyricsLegacy(trackMetadata, options) ?: continue
                            // add empty element at the beginning
                            lrc.add(0, MediaStoreUtils.Lyric())
                            break@loop
                        }
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    mediaSession?.let {
                        lyrics = null
                        lyricsLegacy = lrc
                        it.broadcastCustomCommand(
                            SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY),
                            Bundle.EMPTY
                        )
                    }
                }.join()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lyrics = null
        lastPlayedManager.save()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        lastPlayedManager.save()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        super.onEvents(player, events)
        // if timeline changed, handle shuffle update in onTimelineChanged() instead
        // (onTimelineChanged() runs before both this callback and onShuffleModeEnabledChanged(),
        // which means shuffleFactory != null is not a valid check)
        lyric()
        if (events.contains(EVENT_SHUFFLE_MODE_ENABLED_CHANGED) &&
            shuffleFactory == null && !events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            // when enabling shuffle, re-shuffle lists so that the first index is up to date
            applyShuffleSeed(false) { c -> { CircularShuffleOrder(
                it, c, controller!!.mediaItemCount, Random.nextLong()) } }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        mediaSession!!.setCustomLayout(ImmutableList.of(getRepeatCommand(), getShufflingCommand()))
        if (needsMissingOnDestroyCallWorkarounds()) {
            handler.post { lastPlayedManager.save() }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        super.onTimelineChanged(timeline, reason)
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            shuffleFactory?.let {
                applyShuffleSeed(false, it)
                shuffleFactory = null
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        mediaSession!!.setCustomLayout(ImmutableList.of(getRepeatCommand(), getShufflingCommand()))
        if (needsMissingOnDestroyCallWorkarounds()) {
            handler.post { lastPlayedManager.save() }
        }
    }

    fun startTimer() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                lyric()
                handler.postDelayed(this, 10)
            }
        }
        handler.post(runnable) // 启动定时器
    }

    fun lyric() {
        val getLyricsLegacy = controller?.getLyricsLegacy()
        val getLyrics = controller?.getLyrics()
        updateLyricsLegacy(getLyricsLegacy, getLyrics)
        val highlightedLyric = getCurrentHighlightedLyric()
        if (highlightedLyric != null) {
            sendlyric(highlightedLyric)
        } else {
            null
        }

    }

    private fun sendlyric(Lyrics: String) {
        val lga by lazy { API() }
        if (highLyric != Lyrics ) {
            highLyric = Lyrics
            val isLyricUIEnabled = prefs.getBoolean("lyric_statusbarLyric", true)
            val isLyricgetterapi = prefs.getBoolean("lyric_lyric_getter_api", true)
            if (isLyricUIEnabled == true ){
                sendMeiZuStatusBarLyric(Lyrics)
                if (isLyricgetterapi == true ){
                    lga.sendLyric(Lyrics, extra = ExtraData().apply {
                        packageName = "org.akanework.gramophone"
                        customIcon = true
                        base64Icon = Tools.drawableToBase64(getDrawable(R.drawable.ic_gramophone_monochrome)!!)
                        useOwnMusicController = false
                        delay = 0
                    })
                }
            }
            //Log.d(TAG,"当前高亮歌词: $Lyrics")

        }

    }
    private fun sendMeiZuStatusBarLyric(Lyrics: String) {
        val channelId = "StatusBarLyric"
        val channelName = "MeiZuStatusBarLyric"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_MIN // 设为低优先级，避免打扰用户
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = "魅族状态栏歌词"

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
        notification.setSmallIcon(R.drawable.ic_gramophone_monochrome) // 确保图标存在且有效
            .setContentTitle("魅族状态栏歌词")  // 通知标题，例如歌曲名称
            .setContentText(Lyrics)  // 通知正文，例如歌手和专辑信息
            .setTicker(Lyrics)  // 设置状态栏歌词滚动内容
            .setPriority(NotificationCompat.PRIORITY_MIN)  // 低优先级，避免打扰
            .setOngoing(true)  // 设置为持续通知，用户无法轻易滑掉通知
        val StatusBar = notification.build()
        // 设置自定义 FLAG，确保只更新状态栏歌词，不更新其他内容
        StatusBar.extras.putInt("ticker_icon", R.drawable.ic_gramophone_monochrome)
        StatusBar.extras.putBoolean("ticker_icon_switch", false)
        // 保持状态栏歌词滚动显示
        StatusBar.flags = StatusBar.flags.or(FLAG_ALWAYS_SHOW_TICKER)
        // 只更新 Ticker（歌词），不会更新其他属性
        StatusBar.flags = StatusBar.flags.or(FLAG_ONLY_UPDATE_TICKER)
        // 检查是否需要请求通知权限（针对 Android 13 及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 如果在 Service 中没有权限，我们不能直接请求权限，因此需要在启动 Service 前确保权限已被授予
                // 这里可以记录日志或通知调用方权限没有授予
                Log.e("GramophoneService", "Notification permission not granted.")
                return
            } else {
                // 已经有权限，直接发送通知
                notificationManager.notify(2, StatusBar)
            }
        } else {
            // 低于 Android 13，不需要请求通知权限，直接发送通知
            notificationManager.notify(2, StatusBar)
        }
    }


    private fun updateNewIndex(): Int {
        val filteredList = lyricsLegacy?.filterIndexed { _, lyric ->
            (lyric.timeStamp ?: 0) <= (controller?.currentPosition ?: 0)
        }

        return if (filteredList?.isNotEmpty() == true) {
            filteredList.indices.maxByOrNull {
                filteredList[it].timeStamp ?: 0
            } ?: -1
        } else {
            -1
        }
    }


    fun getCurrentHighlightedLyric(): String? {
        val position = updateNewIndex()
        return if (position != -1) {
            lyricsLegacy?.get(position)?.content // 返回高亮的歌词内容
        } else {
            null // 没有高亮的歌词
        }
    }


    fun updateLyricsLegacy(parsedLyrics: MutableList<MediaStoreUtils.Lyric>?, parsedLyricsa: SemanticLyrics?) {
        lyrics = null
        if ( parsedLyrics.toString() == "null" ){
            lyricsLegacy = SemanticLyrics.convertForLegacy(parsedLyricsa)
            adapter?.updateLyrics(lyricsLegacy)
            newView?.updateLyrics(null)
        } else {
            lyricsLegacy = parsedLyrics
            adapter?.updateLyrics(lyricsLegacy)
            newView?.updateLyrics(null)

        }
    }

    @SuppressLint("MissingPermission", "NotificationPermission") // only used on S/S_V2
    override fun onForegroundServiceStartNotAllowedException() {
        Log.w(TAG, "Failed to resume playback :/")
        if (mayThrowForegroundServiceStartNotAllowed()) {
            nm.notify(NOTIFY_ID, NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID).apply {
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setAutoCancel(true)
                setCategory(NotificationCompat.CATEGORY_ERROR)
                setSmallIcon(R.drawable.ic_error)
                setContentTitle(this@GramophonePlaybackService.getString(R.string.fgs_failed_title))
                setContentText(this@GramophonePlaybackService.getString(R.string.fgs_failed_text))
                setContentIntent(
                    PendingIntent.getActivity(
                        this@GramophonePlaybackService,
                        PENDING_INTENT_NOTIFY_ID,
                        Intent(this@GramophonePlaybackService, MainActivity::class.java)
                            .putExtra(MainActivity.PLAYBACK_AUTO_START_FOR_FGS, true),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                )
                setVibrate(longArrayOf(0L, 200L))
                setLights(0, 0, 0)
                setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                setSound(null)
            }.build())
        } else {
            handler.post {
                throw IllegalStateException("onForegroundServiceStartNotAllowedException shouldn't be called on T+")
            }
        }
    }

    private fun applyShuffleSeed(lazy: Boolean, factory:
        (Int) -> ((CircularShuffleOrder) -> Unit) -> CircularShuffleOrder) {
        if (lazy) {
            shuffleFactory = factory
        } else {
            (mediaSession?.player as EndedWorkaroundPlayer?)?.let {
                val data = try {
                    factory(it.currentMediaItemIndex)
                } catch (e: IllegalStateException) {
                    lastPlayedManager.eraseShuffleOrder()
                    throw e
                }
                it.setShuffleOrder(data)
            }
        }
    }
}