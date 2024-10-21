package org.akanework.gramophone.logic.ui.spans

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import androidx.annotation.ColorInt

class MyForegroundColorSpan(@ColorInt var color: Int) : CharacterStyle(), UpdateAppearance {
	override fun updateDrawState(tp: TextPaint) {
		tp.color = color
	}
}