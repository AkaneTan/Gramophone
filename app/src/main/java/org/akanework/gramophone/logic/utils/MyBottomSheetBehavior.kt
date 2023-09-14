package org.akanework.gramophone.logic.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.OverScroller
import androidx.customview.widget.ViewDragHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.reflect.Field

class MyBottomSheetBehavior<T : View>(context: Context, attrs: AttributeSet)
	: BottomSheetBehavior<T>(context, attrs) {

	companion object {
		fun <T : View> from(v: T): MyBottomSheetBehavior<T> {
			return BottomSheetBehavior.from<T>(v) as MyBottomSheetBehavior<T>
		}
	}

	@SuppressLint("RestrictedApi")
	override fun isHideableWhenDragging(): Boolean {
		return false
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

	fun getViewDragHelper(): ViewDragHelper? =
		BottomSheetUtil.viewDragHelper.get(this) as? ViewDragHelper?

	private fun ViewDragHelper.getScroller(): OverScroller? =
		BottomSheetUtil.mScroller.get(this) as? OverScroller?

	fun setStateWithoutAnimation(state: Int) {
		setState(state)
		getViewDragHelper()!!.getScroller()!!.abortAnimation()
	}

	@SuppressLint("RestrictedApi")
	override fun handleBackInvoked() {
		if (state != STATE_HIDDEN) {
			setHideableInternal(false)
		}
		super.handleBackInvoked()
		setHideableInternal(true)
	}
}


