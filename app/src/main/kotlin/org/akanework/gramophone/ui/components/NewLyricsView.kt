package org.akanework.gramophone.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.spans.StaticLayoutBuilderCompat
import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.akanework.gramophone.logic.utils.SpeakerEntity
import org.akanework.gramophone.ui.MainActivity

/*
== CHECKLIST ==
- horizontal scroll (check:D)
- auto-scroll of lyrics
- text that scales up and changes color when currently active
- left/right alignment for vocal1/vocal2
- colors? for wakaloke ext
- small text for background / translation
- even smaller text for a background translation?
- individual highlighting of words
- begin next line while current isn't complete yet (because that's just how songs work)
 */
class NewLyricsView(context: Context, attrs: AttributeSet) : View(context, attrs) {

	private val defaultTextPaint = TextPaint().apply { color = Color.RED }
	private val translationTextPaint = TextPaint().apply { color = Color.GREEN }
	private val translationBackgroundTextPaint = TextPaint().apply { color = Color.BLUE }
	private var wordActiveSpan: ForegroundColorSpan? = null
	private var spForRender: List<Pair<StaticLayout, SpannableStringBuilder>>? = null
	private var spForMeasure: Pair<Pair<Int, Int>, List<Pair<StaticLayout, SpannableStringBuilder>>>? = null
	private var defaultTextColor = 0
	private var highlightTextColor = 0
	private var lyrics: SemanticLyrics? = null
	private var posForRender = 0uL
	private val activity
		get() = context as MainActivity
	private val instance
		get() = activity.getPlayer()
	private val scrollView
		get() = parent as NestedScrollView

	init {
		defaultTextPaint.textSize = context.resources.getDimension(R.dimen.lyric_text_size)
		translationTextPaint.textSize = context.resources.getDimension(R.dimen.lyric_tl_text_size)
		translationBackgroundTextPaint.textSize = context.resources.getDimension(R.dimen.lyric_tl_bg_text_size)
	}

	fun updateTextColor(newColor: Int, newHighlightColor: Int) {
		if (defaultTextColor != newColor) {
			defaultTextColor = newColor
			defaultTextPaint.color = defaultTextColor
			translationTextPaint.color = defaultTextColor
			translationBackgroundTextPaint.color = defaultTextColor
			invalidate()
		}
		if (highlightTextColor != newHighlightColor) {
			highlightTextColor = newHighlightColor
			wordActiveSpan = null
			invalidate()
		}
	}

	fun updateLyrics(parsedLyrics: SemanticLyrics?) {
		spForRender = null
		spForMeasure = null
		requestLayout()
		lyrics = parsedLyrics
	}

	fun updateLyricPositionFromPlaybackPos() {
		if (instance?.currentPosition != posForRender.toLong())
			invalidate()
	}

	fun onPrefsChanged() {
	}

	override fun onDraw(canvas: Canvas) {
		posForRender = instance?.currentPosition?.toULong() ?: 0uL
		if (spForRender == null) {
			requestLayout()
			return
		}
		canvas.save()
		val lines = if (lyrics is SemanticLyrics.SyncedLyrics)
				(lyrics as SemanticLyrics.SyncedLyrics).text else null
		spForRender!!.forEachIndexed { i, it ->
			var setSpan = false
			val lastWord = lines?.get(i)?.lyric?.words?.lastOrNull()?.timeRange
			val currentLineStarted = lines != null && posForRender > lines[i].lyric.start
			val highlight = lines == null || currentLineStarted && lastWord != null &&
					lastWord.last > posForRender || currentLineStarted &&
					lines.find { it.lyric.start > lines[i].lyric.start
							&& posForRender > it.lyric.start } == null
			if (highlight) {
				canvas.save()
				canvas.scale(1.25f, 1.25f)
				if (wordActiveSpan == null)
					wordActiveSpan = ForegroundColorSpan(highlightTextColor)
				if (lines != null && lines[i].lyric.words != null) {
					val word = lines[i].lyric.words?.findLast { it.timeRange.start <= posForRender }
					if (word != null) {
						it.second.setSpan(wordActiveSpan, 0, word.charRange.last,
							Spanned.SPAN_INCLUSIVE_INCLUSIVE)
						setSpan = true
					}
				} else {
					it.second.setSpan(wordActiveSpan, 0, it.second.length,
						Spanned.SPAN_INCLUSIVE_INCLUSIVE)
					setSpan = true
				}
			} else if (it.first.alignment != Layout.Alignment.ALIGN_NORMAL) {
				canvas.save()
				if (it.first.alignment == Layout.Alignment.ALIGN_OPPOSITE)
					canvas.translate(width * 0.2f, 0f)
				else
					canvas.translate(width * 0.1f, 0f)
			}
			it.first.draw(canvas)
			if (highlight) {
				if (setSpan)
					it.second.removeSpan(wordActiveSpan)
				canvas.restore()
				canvas.translate(0f, it.first.height.toFloat() * 1.25f)
			} else {
				if (it.first.alignment != Layout.Alignment.ALIGN_NORMAL)
					canvas.restore()
				canvas.translate(0f, it.first.height.toFloat())
			}
		}
		canvas.restore()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val myWidth = getDefaultSize(minimumWidth, widthMeasureSpec)
		if (spForMeasure == null || spForMeasure!!.first.first != myWidth)
			spForMeasure = buildSpForMeasure(lyrics, myWidth)
		setMeasuredDimension(myWidth, getDefaultSize(spForMeasure!!.first.second, heightMeasureSpec))
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		if (spForMeasure == null || spForMeasure!!.first.first != right - left
			|| spForMeasure!!.first.second != bottom - top)
			spForMeasure = buildSpForMeasure(lyrics, right - left)
		spForRender = spForMeasure!!.second
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
	}

	fun buildSpForMeasure(lyrics: SemanticLyrics?, width: Int): Pair<Pair<Int, Int>,
			List<Pair<StaticLayout, SpannableStringBuilder>>> {
		val lines = lyrics?.unsyncedText ?: listOf("NO_LYRIC_FOUND")
		val syncedLines = (lyrics as? SemanticLyrics.SyncedLyrics?)?.text
		val spLines = lines.mapIndexed { i, it ->
			val sb = SpannableStringBuilder(it)
			val align = if (syncedLines != null && syncedLines[i].lyric.speaker == SpeakerEntity.Voice2)
				Layout.Alignment.ALIGN_OPPOSITE
			else Layout.Alignment.ALIGN_NORMAL
			val tl = syncedLines != null && syncedLines[i].isTranslated
			val bg = syncedLines != null && syncedLines[i].lyric.speaker == SpeakerEntity.Background
			Pair(StaticLayoutBuilderCompat.obtain(sb,
				if (tl && bg) translationBackgroundTextPaint else if (tl || bg)
					translationTextPaint else defaultTextPaint, (width * 0.8f).toInt())
				.setAlignment(align)
				.build(), sb)
		}
		val heights = spLines.map { it.first.height }
		return Pair(Pair(width, (heights.max() * 0.25f + heights.sum()).toInt()), spLines)
	}
}