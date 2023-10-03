package org.akanework.gramophone.logic.utils

import android.app.Activity
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

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

fun Drawable.startAnimation() {
	when (this) {
		is AnimatedVectorDrawable -> start()
		is AnimatedVectorDrawableCompat -> start()
		else -> throw IllegalArgumentException()
	}
}