package org.akanework.gramophone.logic.utils

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import java.util.Locale

object AnimationUtils {

    val easingInterpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    const val FAST_DURATION = 256L
    const val MID_DURATION = 350L

    inline fun <reified T> createValAnimator(
        fromValue: T,
        toValue: T,
        duration: Long = FAST_DURATION,
        interpolator: TimeInterpolator = easingInterpolator,
        isArgb: Boolean = false,
        crossinline doOnEnd: (() -> Unit) = {},
        crossinline changedListener: (animatedValue: T) -> Unit,
    ) {
        when (T::class) {
            Int::class -> {
                if (!isArgb)
                    ValueAnimator.ofInt(fromValue as Int, toValue as Int)
                else
                    ValueAnimator.ofArgb(fromValue as Int, toValue as Int)
            }
            Float::class -> {
                ValueAnimator.ofFloat(fromValue as Float, toValue as Float)
            }
            else -> throw IllegalArgumentException("No valid animator type found!")
        }.apply {
            this.duration = duration
            this.interpolator = interpolator
            this.addUpdateListener {
                changedListener(this.animatedValue as T)
            }
            this.doOnEnd {
                doOnEnd()
            }
            start()
        }
    }
}