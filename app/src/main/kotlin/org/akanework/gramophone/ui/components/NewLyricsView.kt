package org.akanework.gramophone.ui.components

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.akanework.gramophone.logic.utils.SemanticLyrics

class NewLyricsView(context: Context, attrs: AttributeSet) : View(context, attrs) {

	private var defaultTextColor = 0
	private var contrastTextColor = 0
	private var highlightTextColor = 0
	private var lyrics: SemanticLyrics? = null

	fun updateTextColor(newColorContrast: Int, newColor: Int, newHighlightColor: Int) {
		defaultTextColor = newColor
		contrastTextColor = newColorContrast
		highlightTextColor = newHighlightColor
	}

	fun updateLyrics(parsedLyrics: SemanticLyrics?) {
		lyrics = parsedLyrics
	}

	fun updateLyricPositionFromPlaybackPos() {
	}

	fun onPrefsChanged() {
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
	}
}