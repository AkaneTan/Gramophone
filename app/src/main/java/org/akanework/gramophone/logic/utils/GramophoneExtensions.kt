package org.akanework.gramophone.logic.utils

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.OverScroller
import androidx.customview.widget.ViewDragHelper
import androidx.media3.session.MediaController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.reflect.Field

/**
 * This file contains some extension methods that made
 * For Gramophone.
 */
val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
fun MediaController.playOrPause() {
	if (isPlaying) {
		pause()
	} else {
		play()
	}
}