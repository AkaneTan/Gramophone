package org.akanework.gramophone.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.TypefaceCompat
import androidx.core.text.getSpans
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.ui.spans.MyForegroundColorSpan
import org.akanework.gramophone.logic.ui.spans.MyGradientSpan
import org.akanework.gramophone.logic.ui.spans.StaticLayoutBuilderCompat
import org.akanework.gramophone.logic.utils.CalculationUtils.lerp
import org.akanework.gramophone.logic.utils.CalculationUtils.lerpInv
import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.akanework.gramophone.logic.utils.SpeakerEntity
import org.akanework.gramophone.ui.MainActivity
import kotlin.math.max
import kotlin.math.min

// TODO colors for wakaloke ext
// TODO react to clicks
class NewLyricsView(context: Context, attrs: AttributeSet) : View(context, attrs) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val grdWidth = 20.dpToPx(context) // TODO unhardcode?
	private val smallSizeFactor = 0.97f
	private val scaleInAnimTime = 650f / 2 // TODO maybe reduce this
	private val scaleColorInterpolator = PathInterpolator(0.4f, 0.2f, 0f, 1f)
	private val defaultTextPaint = TextPaint().apply { color = Color.RED }
	private val translationTextPaint = TextPaint().apply { color = Color.GREEN }
	private val translationBackgroundTextPaint = TextPaint().apply { color = Color.BLUE }
	private var wordActiveSpan = MyForegroundColorSpan(Color.CYAN)
	private val bounds = Rect()
	private var gradientSpanPool = mutableListOf<MyGradientSpan>()
	private var colorSpanPool = mutableListOf<MyForegroundColorSpan>()
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
		var changed = false
		if (defaultTextColor != newColor) {
			defaultTextColor = newColor
			defaultTextPaint.color = defaultTextColor
			translationTextPaint.color = defaultTextColor
			translationBackgroundTextPaint.color = defaultTextColor
			changed = true
		}
		if (highlightTextColor != newHighlightColor) {
			highlightTextColor = newHighlightColor
			wordActiveSpan.color = highlightTextColor
			changed = true
		}
		if (changed) {
			spForRender?.forEach { it.text.getSpans<MyGradientSpan>()
				.forEach { s -> it.text.removeSpan(s) }}
			gradientSpanPool.clear()
			(1..3).forEach { gradientSpanPool.add(makeGradientSpan()) }
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
		posForRender = instance?.currentPosition?.toULong() ?: 0uL // TODO do some mp3s go backwards sometimes lol??
		if (spForRender == null) {
			requestLayout()
			return
		}
		var animating = false
		canvas.save()
		val lines = if (lyrics is SemanticLyrics.SyncedLyrics)
				(lyrics as SemanticLyrics.SyncedLyrics).text else null
		spForRender!!.forEachIndexed { i, it ->
			var spanEnd = -1
			var spanStartGradient = -1
			var gradientProgress = Float.NEGATIVE_INFINITY
			val firstTs = lines?.get(i)?.lyric?.start ?: ULong.MIN_VALUE
			val lastTs = lines?.get(i)?.lyric?.words?.lastOrNull()?.timeRange?.last ?: lines
				?.find { it.lyric.start > lines[i].lyric.start }?.lyric?.start ?: Long.MAX_VALUE.toULong()
			val timeOffsetForUse = min(scaleInAnimTime, min(lerp(firstTs.toFloat(), lastTs.toFloat(),
				0.5f) - firstTs.toFloat(), firstTs.toFloat()))
			val highlight = posForRender >= firstTs - timeOffsetForUse.toULong() &&
					posForRender <= lastTs + timeOffsetForUse.toULong()
			val scaleInProgress = if (lines == null) 1f else lerpInv(firstTs.toFloat() -
					timeOffsetForUse, firstTs.toFloat() + timeOffsetForUse, posForRender.toFloat())
			val scaleOutProgress = if (lines == null) 1f else lerpInv(lastTs.toFloat() -
					timeOffsetForUse, lastTs.toFloat() + timeOffsetForUse, posForRender.toFloat())
			val hlScaleFactor = if (lines == null) smallSizeFactor else {
				// lerp() argument order is swapped because we divide by this factor
				if (scaleOutProgress >= 0f && scaleOutProgress <= 1f)
					lerp(smallSizeFactor, 1f, scaleColorInterpolator.getInterpolation(scaleOutProgress))
				else if (scaleInProgress >= 0f && scaleInProgress <= 1f)
					lerp(1f, smallSizeFactor, scaleColorInterpolator.getInterpolation(scaleInProgress))
				else if (highlight)
					smallSizeFactor
				else 1f
			}
			if ((scaleInProgress >= -.1f && scaleInProgress <= 1f) ||
				(scaleOutProgress >= -.1f && scaleOutProgress <= 1f))
				animating = true
			canvas.translate(0f, it.paddingTop.toFloat() / hlScaleFactor)
			if (highlight) {
				canvas.save()
				canvas.scale(1f / hlScaleFactor, 1f / hlScaleFactor)
				if (lines != null && lines[i].lyric.words != null) {
					val word = lines[i].lyric.words?.findLast { it.timeRange.start <= posForRender }
					if (word != null) {
						spanEnd = word.charRange.last + 1 // get exclusive end
						val gradientEndTime = min(lastTs.toFloat() - timeOffsetForUse,
							word.timeRange.last.toFloat())
						val gradientStartTime = min(max(word.timeRange.start.toFloat(),
							firstTs.toFloat() - timeOffsetForUse), gradientEndTime - 1f)
						gradientProgress = lerpInv(gradientStartTime, gradientEndTime,
							posForRender.toFloat())
						if (gradientProgress >= 0f && gradientProgress <= 1f)
							spanStartGradient = word.charRange.first
					}
				} else {
					spanEnd = it.text.length
				}
			}
			if (it.layout.alignment != Layout.Alignment.ALIGN_NORMAL) {
				if (!highlight)
					canvas.save()
				if (it.layout.alignment == Layout.Alignment.ALIGN_OPPOSITE)
					canvas.translate(width * (1 - smallSizeFactor / hlScaleFactor), 0f)
				else // Layout.Alignment.ALIGN_CENTER
					canvas.translate(width * ((1 - smallSizeFactor / hlScaleFactor) / 2), 0f)
			}
			if (gradientProgress >= -.1f && gradientProgress <= 1f)
				animating = true
			val spanEndWithoutGradient = if (spanStartGradient == -1) spanEnd else spanStartGradient
			val inColorAnim = (scaleInProgress >= 0f && scaleInProgress <= 1f && gradientProgress ==
					Float.NEGATIVE_INFINITY) || (scaleOutProgress >= 0f && scaleOutProgress <= 1f)
			var colorSpan = it.text.getSpans<MyForegroundColorSpan>().firstOrNull()
			val cachedEnd = colorSpan?.let { j -> it.text.getSpanStart(j) } ?: -1
			val col = if (inColorAnim) ColorUtils.blendARGB(if (scaleOutProgress >= 0f &&
				scaleOutProgress <= 1f) highlightTextColor else defaultTextColor,
				if (scaleInProgress >= 0f && scaleInProgress <= 1f &&
					gradientProgress == Float.NEGATIVE_INFINITY) highlightTextColor
				else defaultTextColor, if (scaleOutProgress >= 0f &&
					scaleOutProgress <= 1f) scaleOutProgress else scaleInProgress) else 0
			if (cachedEnd != spanEndWithoutGradient || inColorAnim != (colorSpan != wordActiveSpan)) {
				if (cachedEnd != -1) {
					it.text.removeSpan(colorSpan!!)
					if (colorSpan != wordActiveSpan && (!inColorAnim || spanEndWithoutGradient == -1)) {
						if (colorSpanPool.size < 10)
							colorSpanPool.add(colorSpan)
						colorSpan = null
					} else if (inColorAnim && colorSpan == wordActiveSpan)
						colorSpan = null
				}
				if (spanEndWithoutGradient != -1) {
					if (inColorAnim && colorSpan == null)
						colorSpan = colorSpanPool.getOrElse(0) { MyForegroundColorSpan(col) }
					else if (!inColorAnim)
						colorSpan = wordActiveSpan
					it.text.setSpan(
						colorSpan, 0, spanEndWithoutGradient,
						Spanned.SPAN_INCLUSIVE_INCLUSIVE
					)
				}
			}
			if (inColorAnim && spanEndWithoutGradient != -1) {
				if (colorSpan!! == wordActiveSpan)
					throw IllegalStateException("colorSpan == wordActiveSpan")
				colorSpan.color = col
			}
			var gradientSpan = it.text.getSpans<MyGradientSpan>().firstOrNull()
			val gradientSpanStart = gradientSpan?.let { j -> it.text.getSpanStart(j) } ?: -1
			if (gradientSpanStart != spanStartGradient) {
				if (gradientSpanStart != -1) {
					it.text.removeSpan(gradientSpan!!)
					if (spanStartGradient == -1) {
						if (gradientSpanPool.size < 10)
							gradientSpanPool.add(gradientSpan)
						gradientSpan = null
					}
				}
				if (spanStartGradient != -1) {
					if (gradientSpan == null)
						gradientSpan = gradientSpanPool.getOrElse(0) { makeGradientSpan() }
					it.text.setSpan(gradientSpan, spanStartGradient, spanEnd,
						Spanned.SPAN_INCLUSIVE_INCLUSIVE)
				}
			}
			if (gradientSpan != null) {
				gradientSpan.lineCount = 0
				gradientSpan.lineOffsets.clear()
				val firstLine = it.layout.getLineForOffset(spanStartGradient)
				val lastLine = it.layout.getLineForOffset(spanEnd)
				for (line in firstLine..lastLine) {
					if (line == firstLine && it.layout.alignment != Layout.Alignment.ALIGN_OPPOSITE) {
						it.layout.paint.getTextBounds(it.text.toString(),
							it.layout.getLineStart(line), spanStartGradient, bounds)
						gradientSpan.lineOffsets.add(bounds.width())
					} else gradientSpan.lineOffsets.add(0)
					if (it.layout.alignment == Layout.Alignment.ALIGN_OPPOSITE) {
						it.layout.paint.getTextBounds(
							it.text.toString(), max(
								spanStartGradient,
								it.layout.getLineStart(line)
							), it.layout.getLineEnd(line), bounds
						)
						gradientSpan.lineOffsets[gradientSpan.lineOffsets.size - 1] = gradientSpan
							.lineOffsets.last() + (width - bounds.width())
					} else if (it.layout.alignment == Layout.Alignment.ALIGN_CENTER) {
						it.layout.paint.getTextBounds(
							it.text.toString(), it.layout.getLineStart(line),
							it.layout.getLineEnd(line), bounds
						)
						gradientSpan.lineOffsets[gradientSpan.lineOffsets.size - 1] = gradientSpan
							.lineOffsets.last() + ((width - bounds.width()) / 2)
					}
					gradientSpan.lineOffsets.add(it.layout.getLineBottom(line) - it.layout.getLineTop(line))
					it.layout.paint.getTextBounds(it.text.toString(), max(spanStartGradient,
						it.layout.getLineStart(line)), min(spanEnd, it.layout.getLineEnd(line)), bounds)
					gradientSpan.lineOffsets.add(bounds.width())
					gradientSpan.lineOffsets.add(max(spanStartGradient, it.layout.getLineStart(line)) - spanStartGradient)
					gradientSpan.lineOffsets.add(min(it.layout.getLineEnd(line), spanEnd) - spanStartGradient)
				}
				gradientSpan.lineOffsets.add(spanEnd - spanStartGradient)
				gradientSpan.lineOffsets.add(if (it.layout.alignment != Layout.Alignment.ALIGN_NORMAL) 2 else 1)
				gradientSpan.progress = gradientProgress
			}
			it.layout.draw(canvas)
			if (highlight || it.layout.alignment != Layout.Alignment.ALIGN_NORMAL)
				canvas.restore()
			canvas.translate(0f, (it.layout.height.toFloat() + it.paddingBottom) / hlScaleFactor)
		}
		canvas.restore()
		if (animating)
			invalidate()
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
		invalidate()
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
		return Pair(Pair(width, (heights.max() * (1 - (1 / smallSizeFactor)) + heights.sum()).toInt()), spLines)
	}

	data class SbItem(val layout: StaticLayout, val text: SpannableStringBuilder, val paddingTop: Int, val paddingBottom: Int)

	private fun makeGradientSpan() = MyGradientSpan(grdWidth, defaultTextColor, highlightTextColor)
}