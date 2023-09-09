package org.akanework.gramophone.logic.services

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.akanework.gramophone.Constants
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R

/**
 * [GramophonePlaybackService] is a server service.
 * It's using exoplayer2 as its player backend.
 */
@UnstableApi
class GramophonePlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        // Create an exoplayer2 instance here for server side.
        val player = ExoPlayer.Builder(this).build()

        val callback = CustomMediaSessionCallback()
        // Create a mediaSession here so we can connect to our
        // client later.
        mediaSession =
            MediaSession
                .Builder(this, player)
                .setCallback(callback)
                .setSessionActivity(
                    getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
                    ),
                ).build()

        // Set AudioAttributes here so media3 can manage audio
        // focus correctly.
        val audioAttributes: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
        player.setAudioAttributes(audioAttributes, true)

        super.onCreate()
    }

    override fun onDestroy() {
        // When destroying, we should release server side player
        // alongside with the mediaSession.
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        super.onDestroy()
    }

    // This onGetSession is a necessary method override needed by
    // MediaSessionService.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private inner class CustomMediaSessionCallback: MediaSession.Callback {
        // Configure commands available to the controller in onConnect()
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands =
                connectionResult.availableSessionCommands
                    .buildUpon()
                    // Add custom commands
                    .add(SessionCommand(Constants.PLAYBACK_SHUFFLE_ACTION, Bundle()))
                    .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands, connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == Constants.PLAYBACK_SHUFFLE_ACTION) {
                // Do custom logic here
                session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS)
                )
            }
            return Futures.immediateFuture(
                SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
            )
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            super.onPostConnect(session, controller)

            val shuffleButton = CommandButton.Builder()
                .setDisplayName("Save to favorites")
                .setIconResId(R.drawable.ic_shuffle)
                .setSessionCommand(SessionCommand(Constants.PLAYBACK_SHUFFLE_ACTION, Bundle()))
                .build()

            // Pass in a list of the controls that the client app should display to users. The arrangement
            // of these controls in the UI is managed by the client app.
            session.setCustomLayout(controller, listOf(shuffleButton))

        }
    }
}
