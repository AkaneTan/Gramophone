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
import com.google.android.material.bottomsheet.BottomSheetBehavior

class CustomBottomSheetBehavior<T : View>(context: Context, attrs: AttributeSet) :
    BottomSheetBehavior<T>(context, attrs) {

    companion object {
        fun <T : View> from(v: T): CustomBottomSheetBehavior<T> {
            return BottomSheetBehavior.from<T>(v) as CustomBottomSheetBehavior<T>
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

    @SuppressLint("RestrictedApi")
    override fun handleBackInvoked() {
        if (state != STATE_HIDDEN) {
            setHideableInternal(false)
        }
        super.handleBackInvoked()
        setHideableInternal(true)
    }
}