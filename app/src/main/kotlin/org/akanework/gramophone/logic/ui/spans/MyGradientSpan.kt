package org.akanework.gramophone.logic.ui.spans

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import org.akanework.gramophone.logic.utils.CalculationUtils.lerp
import org.akanework.gramophone.logic.utils.CalculationUtils.lerpInv
import kotlin.math.max
import kotlin.math.min

// Hacks, hacks, hacks...
class MyGradientSpan(val grdWidth: Int, color: Int, highlightColor: Int) : CharacterStyle(), UpdateAppearance {
	private val matrix = Matrix()
	private var shader = LinearGradient(
		0f, 50f, grdWidth.toFloat(), 50f,
		highlightColor, color,
		Shader.TileMode.CLAMP
	)
	var progress = 1f
	var lineOffsets = mutableListOf<Int>()
	var lineCount = 0
	override fun updateDrawState(tp: TextPaint) {
		tp.color = Color.WHITE
		val ourProgress = max(0f, min(1f, lerpInv(lineOffsets[5*lineCount+3].toFloat(), lineOffsets[
			5*lineCount+4].toFloat(), lerp(0f, lineOffsets[lineOffsets.size-1].toFloat(), progress))))
		shader.setLocalMatrix(matrix.apply {
			reset()
			postTranslate(lineOffsets[lineCount*5].toFloat() + ((lineOffsets[5*lineCount+2]
					+ (grdWidth * 3)) * ourProgress) - (grdWidth * 2), 0f)
			postScale(1f, lineOffsets[lineCount*5+1] / 100f)
		})
		tp.shader = shader
		lineCount++
	}
}