package org.akanework.gramophone.ui.components

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

private inline val padding
	get() = 160f
private inline val size
	get() = 960f
private inline val barWidth
	get() = 160f

class NowPlayingDrawable : Drawable() {

	private val paint = Paint()
	private var state: Int = 0 // 0 = paused, 1 = playing
	private var sx: Float = 1f // scale x
	private var sy: Float = 1f // scale y
	private var lc: Float = 0f // left current
	private var lt: Float = 0f // left target
	private var mc: Float = 0f // middle current
	private var mt: Float = 0f // middle target
	private var rc: Float = 0f // right current
	private var rt: Float = 0f // right target

	override fun draw(canvas: Canvas) {
		// Left bar
		canvas.drawBar(0f, 320f)

		// Middle bar
		canvas.drawBar(240f, 640f)

		// Right bar
		canvas.drawBar(480f, 480f)

		// invalidateSelf()
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun Canvas.drawBar(left: Float, height: Float) {
		drawRect((padding + left) * sx, ((size - padding) - height) * sy,
			(padding + left + barWidth) * sx, (size - padding) * sy, paint)
	}

	override fun onBoundsChange(bounds: Rect) {
		sx = bounds.width() / size
		sy = bounds.height() / size
		tr.set(0, 0, (size * sx).toInt(), (size * sy).toInt())
		tr.op(
			(padding * sx).toInt(), (padding * sy).toInt(), ((size - padding) * sx).toInt(),
			((size - padding) * sy).toInt(), Region.Op.DIFFERENCE
		)
	}

	// --- lots of boilerplate below ---
	@ColorInt private var tintColor: Int? = null
	private var tintMode: PorterDuff.Mode? = null
	private val tr = Region()

	override fun getTransparentRegion(): Region {
		return tr
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun getAlpha(): Int {
		return paint.alpha
	}

	override fun setTintList(tint: ColorStateList?) {
		setTint(tint?.getColorForState(null, Color.BLACK) ?: Color.BLACK)
	}

	override fun setTint(tintColor: Int) {
		this.tintColor = tintColor
		paint.colorFilter = if (tintMode == null) null else
			PorterDuffColorFilter(tintColor, tintMode!!)
		paint.color = if (tintMode == null) tintColor else Color.WHITE
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		this.tintColor = null
		this.tintMode = null
		paint.color = if (colorFilter == null) Color.BLACK else Color.WHITE
		paint.colorFilter = colorFilter
	}

	override fun getColorFilter(): ColorFilter? {
		return if (tintMode != null) null else paint.colorFilter
	}

	override fun setTintMode(tintMode: PorterDuff.Mode?) {
		this.tintMode = tintMode
		paint.colorFilter = if (tintColor == null || tintMode == null) null else
			PorterDuffColorFilter(tintColor!!, tintMode)
		paint.color = if (tintColor == null) Color.BLACK else
			if (tintMode == null) tintColor!! else Color.WHITE
	}

	override fun onLevelChange(level: Int): Boolean {
		this.state = level
		return true
	}

	@Deprecated("Deprecated in Java",
		ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
	)
	override fun getOpacity(): Int =
		// Must be PixelFormat.UNKNOWN, TRANSLUCENT, TRANSPARENT, or OPAQUE
		PixelFormat.TRANSLUCENT
}
