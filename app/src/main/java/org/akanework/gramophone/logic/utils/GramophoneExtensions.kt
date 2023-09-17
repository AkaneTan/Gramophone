package org.akanework.gramophone.logic.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.OverScroller
import androidx.customview.widget.ViewDragHelper
import androidx.media3.common.MediaItem
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

fun MediaItem.getUri(): Uri {
	return localConfiguration!!.uri
}

fun Activity.closeKeyboard() {
	if (currentFocus != null) {
		val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
	}
}

fun View.showSoftKeyboard() {
	if (requestFocus()) {
		val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
	}
}
