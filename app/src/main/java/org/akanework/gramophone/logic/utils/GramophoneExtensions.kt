package org.akanework.gramophone.logic.utils

import android.content.res.Resources
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController

/**
 * This file contains some extension methods that made
 * For Gramophone.
 */
val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
fun MediaController.playOrPause() {
	if (isPlaying) {
		pause()
	} else {
		play()
	}
}

fun MediaItem.getUri(): Uri {
	return localConfiguration!!.uri
}