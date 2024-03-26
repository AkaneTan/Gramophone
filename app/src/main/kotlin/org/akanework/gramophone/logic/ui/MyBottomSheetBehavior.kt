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

package org.akanework.gramophone.logic.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.customview.widget.ViewDragHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.reflect.Field

class MyBottomSheetBehavior<T : View>(context: Context, attrs: AttributeSet) :
    BottomSheetBehavior<T>(context, attrs) {

    companion object {
        fun <T : View> from(v: T): MyBottomSheetBehavior<T> {
            return BottomSheetBehavior.from<T>(v) as MyBottomSheetBehavior<T>
        }
    }

    init {
        state = STATE_HIDDEN
        maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
        isGestureInsetBottomIgnored = true
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

    private fun getViewDragHelper(): ViewDragHelper? =
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

    override fun onLayoutChild(parent: CoordinatorLayout, child: T, layoutDirection: Int): Boolean {
        val value = super.onLayoutChild(parent, child, layoutDirection)
        /*ViewCompat.setWindowInsetsAnimationCallback(child, object : WindowInsetsAnimationCompat.Callback(
            DISPATCH_MODE_CONTINUE_ON_SUBTREE
        ) {
            override fun onProgress(
                p0: WindowInsetsCompat,
                p1: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                TODO("Not yet implemented")
            }

        })
        // TODO y no work*/
        return value
    }
}


