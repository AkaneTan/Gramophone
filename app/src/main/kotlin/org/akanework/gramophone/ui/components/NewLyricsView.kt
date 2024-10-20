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
import androidx.core.graphics.TypefaceCompat
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.ui.spans.StaticLayoutBuilderCompat
import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.akanework.gramophone.logic.utils.SpeakerEntity
import org.akanework.gramophone.ui.MainActivity

// TODO colors for wakaloke ext
// TODO react to clicks
// TODO animations
class NewLyricsView(context: Context, attrs: AttributeSet) : View(context, attrs) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val smallSizeFactor = 0.97f
	private val defaultTextPaint = TextPaint().apply { color = Color.RED }
	private val translationTextPaint = TextPaint().apply { color = Color.GREEN }
	private val translationBackgroundTextPaint = TextPaint().apply { color = Color.BLUE }
	private var wordActiveSpan: ForegroundColorSpan? = null
	private var spForRender: List<SbItem>? = null
	private var spForMeasure: Pair<Pair<Int, Int>, List<SbItem>>? = null
	private var defaultTextColor = 0
	private var highlightTextColor = 0
	private var lyrics: SemanticLyrics? = null
	private var posForRender = 0uL
	private val activity
		get() = context as MainActivity
	private val instance
		get() = activity.getPlayer()
	private val scrollView // TODO autoscroll
		get() = parent as NestedScrollView

	init {
		defaultTextPaint.textSize = context.resources.getDimension(R.dimen.lyric_text_size)
		translationTextPaint.textSize = context.resources.getDimension(R.dimen.lyric_tl_text_size)
		translationBackgroundTextPaint.textSize = context.resources.getDimension(R.dimen.lyric_tl_bg_text_size)
		applyTypefaces()
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
		applyTypefaces()
		spForRender = null
		spForMeasure = null
		requestLayout()
	}

	private fun applyTypefaces() {
		val typeface = if (prefs.getBooleanStrict("lyric_bold", false)) {
			TypefaceCompat.create(context, null, 700, false)
		} else {
			TypefaceCompat.create(context, null, 500, false)
		}
		defaultTextPaint.typeface = typeface
		translationTextPaint.typeface = typeface
		translationBackgroundTextPaint.typeface = typeface
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
			if (!highlight)
				canvas.translate(0f, it.paddingTop.toFloat())
			if (highlight) {
				canvas.translate(0f, it.paddingTop.toFloat() / smallSizeFactor)
				canvas.save()
				canvas.scale(1f / smallSizeFactor, 1f / smallSizeFactor)
				if (wordActiveSpan == null)
					wordActiveSpan = ForegroundColorSpan(highlightTextColor)
				if (lines != null && lines[i].lyric.words != null) {
					val word = lines[i].lyric.words?.findLast { it.timeRange.start <= posForRender }
					if (word != null) {
						it.text.setSpan(wordActiveSpan, 0, word.charRange.last,
							Spanned.SPAN_INCLUSIVE_INCLUSIVE)
						setSpan = true
					}
				} else {
					it.text.setSpan(wordActiveSpan, 0, it.text.length,
						Spanned.SPAN_INCLUSIVE_INCLUSIVE)
					setSpan = true
				}
			} else if (it.layout.alignment != Layout.Alignment.ALIGN_NORMAL) {
				canvas.save()
				if (it.layout.alignment == Layout.Alignment.ALIGN_OPPOSITE)
					canvas.translate(width * (1 - smallSizeFactor), 0f)
				else
					canvas.translate(width * ((1 - smallSizeFactor) / 2), 0f)
			}
			it.layout.draw(canvas)
			val th = it.layout.height.toFloat() + it.paddingBottom
			if (highlight) {
				if (setSpan)
					it.text.removeSpan(wordActiveSpan)
				canvas.restore()
				canvas.translate(0f, th / smallSizeFactor)
			} else {
				if (it.layout.alignment != Layout.Alignment.ALIGN_NORMAL)
					canvas.restore()
				canvas.translate(0f, th)
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

	fun buildSpForMeasure(lyrics: SemanticLyrics?, width: Int): Pair<Pair<Int, Int>, List<SbItem>> {
		val lines = lyrics?.unsyncedText ?: listOf("NO_LYRIC_FOUND") // TODO NO_LYRIC_FOUND
		val syncedLines = (lyrics as? SemanticLyrics.SyncedLyrics?)?.text
		val spLines = lines.mapIndexed { i, it ->
			val sb = SpannableStringBuilder(it)
			val align = if (prefs.getBooleanStrict("lyric_center", false))
				Layout.Alignment.ALIGN_CENTER
			else if (syncedLines != null && syncedLines[i].lyric.speaker == SpeakerEntity.Voice2)
				Layout.Alignment.ALIGN_OPPOSITE
			else Layout.Alignment.ALIGN_NORMAL
			val tl = syncedLines != null && syncedLines[i].isTranslated
			val bg = syncedLines != null && syncedLines[i].lyric.speaker == SpeakerEntity.Background
			val paddingTop = if (syncedLines?.get(i)?.isTranslated == true) 2 else 18
			val paddingBottom = if (i + 1 < (syncedLines?.size ?: -1) &&
				syncedLines?.get(i + 1)?.isTranslated == true) 2 else 18
			SbItem(StaticLayoutBuilderCompat.obtain(sb,
				if (tl && bg) translationBackgroundTextPaint else if (tl || bg)
					translationTextPaint else defaultTextPaint, (width * smallSizeFactor).toInt())
				.setAlignment(align)
				.build(), sb, paddingTop.dpToPx(context), paddingBottom.dpToPx(context))
		}
		val heights = spLines.map { it.layout.height + it.paddingTop + it.paddingBottom }
		return Pair(Pair(width, (heights.max() / smallSizeFactor + heights.sum()).toInt()), spLines)
	}

	data class SbItem(val layout: StaticLayout, val text: SpannableStringBuilder, val paddingTop: Int, val paddingBottom: Int)
}