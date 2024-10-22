package org.akanework.gramophone.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.graphics.TypefaceCompat
import androidx.core.widget.NestedScrollView
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.ui.spans.MyForegroundColorSpan
import org.akanework.gramophone.logic.ui.spans.StaticLayoutBuilderCompat
import org.akanework.gramophone.logic.utils.CalculationUtils.lerp
import org.akanework.gramophone.logic.utils.CalculationUtils.lerpInv
import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.akanework.gramophone.logic.utils.SpeakerEntity
import org.akanework.gramophone.ui.MainActivity
import kotlin.math.min

// TODO colors for wakaloke ext
// TODO react to clicks
// TODO color animations
class NewLyricsView(context: Context, attrs: AttributeSet) : View(context, attrs) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val smallSizeFactor = 0.97f
	private val scaleInAnimTime = 650f / 2
	private val scaleColorInterpolator = PathInterpolator(0.4f, 0.2f, 0f, 1f)
	private val defaultTextPaint = TextPaint().apply { color = Color.RED }
	private val translationTextPaint = TextPaint().apply { color = Color.GREEN }
	private val translationBackgroundTextPaint = TextPaint().apply { color = Color.BLUE }
	private var wordActiveSpan = MyForegroundColorSpan(Color.CYAN)
	private var spForRender: List<SbItem>? = null
	private var spForMeasure: Pair<Pair<Int, Int>, List<SbItem>>? = null
	private val spSpanCache = hashMapOf<SpannableStringBuilder, Int>()
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
			wordActiveSpan.color = highlightTextColor
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
		var animating = false
		canvas.save()
		val lines = if (lyrics is SemanticLyrics.SyncedLyrics)
				(lyrics as SemanticLyrics.SyncedLyrics).text else null
		spForRender!!.forEachIndexed { i, it ->
			var spanEnd: Int? = null
			val firstTs = lines?.get(i)?.lyric?.start ?: ULong.MIN_VALUE
			val lastTs = lines?.get(i)?.lyric?.words?.lastOrNull()?.timeRange?.last ?: lines
				?.find { it.lyric.start > lines[i].lyric.start }?.lyric?.start ?: Long.MAX_VALUE.toULong()
			val timeOffsetForUse = min(scaleInAnimTime, lerp(firstTs.toFloat(), lastTs.toFloat(),
				0.5f) - firstTs.toFloat())
			val highlight = posForRender >= firstTs - timeOffsetForUse.toULong() &&
					posForRender <= lastTs + timeOffsetForUse.toULong()
			val scaleInProgress = if (lines == null) 1f else lerpInv(firstTs.toFloat() -
					timeOffsetForUse, firstTs.toFloat() + timeOffsetForUse, posForRender.toFloat())
			val scaleOutProgress = if (lines == null) 1f else lerpInv(lastTs.toFloat() -
					timeOffsetForUse, lastTs.toFloat() + timeOffsetForUse, posForRender.toFloat())
			val hlScaleFactor = if (lines == null) smallSizeFactor else {
				// lerp argument order is swapped because we divide by this factor
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
					if (word != null)
						spanEnd = word.charRange.last + 1
				} else {
					spanEnd = it.text.length
				}
			} else if (it.layout.alignment != Layout.Alignment.ALIGN_NORMAL) {
				canvas.save()
				if (it.layout.alignment == Layout.Alignment.ALIGN_OPPOSITE)
					canvas.translate(width * (1 - hlScaleFactor), 0f)
				else
					canvas.translate(width * ((1 - hlScaleFactor) / 2), 0f)
			}
			val cachedEnd = spSpanCache[it.text]
			if (cachedEnd != spanEnd) {
				if (cachedEnd != null)
					it.text.removeSpan(wordActiveSpan)
				if (spanEnd != null) {
					it.text.setSpan(
						wordActiveSpan, 0, spanEnd,
						Spanned.SPAN_INCLUSIVE_INCLUSIVE
					)
					spSpanCache[it.text] = spanEnd
				} else
					spSpanCache.remove(it.text)
			}
			it.layout.draw(canvas)
			val th = it.layout.height.toFloat() + it.paddingBottom
			if (highlight || it.layout.alignment != Layout.Alignment.ALIGN_NORMAL)
				canvas.restore()
			canvas.translate(0f, th / hlScaleFactor)
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
}