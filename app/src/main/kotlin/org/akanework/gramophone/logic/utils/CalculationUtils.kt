package org.akanework.gramophone.logic.utils

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.graphics.ColorUtils
import kotlin.math.min

/**
 * [CalculationUtils] contains some methods for internal
 * calculation.
 */
object CalculationUtils {

    /**
     * [convertDurationToTimeStamp] makes a string format
     * of duration (presumably long) converts into timestamp
     * like 300 to 5:00.
     *
     * @param duration
     * @return
     */
    fun convertDurationToTimeStamp(duration: Long): String {
        val minutes = duration / 1000 / 60
        val seconds = duration / 1000 - minutes * 60
        if (seconds < 10) {
            return "$minutes:0$seconds"
        }
        return "$minutes:$seconds"
    }

    /**
     * Set the alpha component of `color` to be `alpha`.
     */
    @ColorInt
    fun setAlphaComponent(
        @ColorInt color: Int,
        @IntRange(from = 0x0, to = 0xFF) alpha: Int
    ): Int {
        require(!(alpha < 0 || alpha > 255)) { "alpha must be between 0 and 255." }
        return color and 0x00ffffff or (alpha shl 24)
    }

    fun constrain(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else amount.coerceAtMost(high)
    }


    fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    fun lerp(start: Int, stop: Int, amount: Float): Float {
        return lerp(start.toFloat(), stop.toFloat(), amount)
    }

    /**
     * Returns the interpolation scalar (s) that satisfies the equation: `value = `[ ][.lerp]`(a, b, s)`
     *
     *
     * If `a == b`, then this function will return 0.
     */
    fun lerpInv(a: Float, b: Float, value: Float): Float {
        return if (a != b) (value - a) / (b - a) else 0.0f
    }

    /** Returns the single argument constrained between [0.0, 1.0].  */
    fun saturate(value: Float): Float {
        return constrain(value, 0.0f, 1.0f)
    }

    /** Returns the saturated (constrained between [0, 1]) result of [.lerpInv].  */
    fun lerpInvSat(a: Float, b: Float, value: Float): Float {
        return saturate(lerpInv(a, b, value))
    }

}
