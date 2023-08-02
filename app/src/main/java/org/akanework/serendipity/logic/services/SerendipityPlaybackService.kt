package org.akanework.serendipity.logic.services

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

class SerendipityPlaybackService : MediaLibraryService() {
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        TODO("Not yet implemented")
    }
}