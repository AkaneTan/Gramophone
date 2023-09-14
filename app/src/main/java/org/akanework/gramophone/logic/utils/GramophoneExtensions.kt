package org.akanework.gramophone.logic.utils

import android.content.res.Resources
import android.widget.OverScroller
import androidx.customview.widget.ViewDragHelper
import androidx.media3.session.MediaController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.reflect.Field

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

// based on https://stackoverflow.com/a/63474805
private object BottomSheetUtil {
	val viewDragHelper: Field = BottomSheetBehavior::class.java
		.getDeclaredField("viewDragHelper")
		.apply { isAccessible = true }
	val mScroller: Field = ViewDragHelper::class.java
		.getDeclaredField("mScroller")
		.apply { isAccessible = true }
}

private fun BottomSheetBehavior<*>.getViewDragHelper(): ViewDragHelper? =
	BottomSheetUtil.viewDragHelper.get(this) as? ViewDragHelper?

private fun ViewDragHelper.getScroller(): OverScroller? =
	BottomSheetUtil.mScroller.get(this) as? OverScroller?

fun BottomSheetBehavior<*>.setStateWithoutAnimation(state: Int) {
	setState(state)
	getViewDragHelper()?.getScroller()?.abortAnimation()
}