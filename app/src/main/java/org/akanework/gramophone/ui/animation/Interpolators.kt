/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.akanework.gramophone.ui.animation

import android.graphics.Path
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.exp
import kotlin.math.ln

/**
 * Utility class to receive interpolators from.
 *
 * Make sure that changes made to this class are also reflected in InterpolatorsAndroidX.
 * Please consider using the androidx dependencies featuring better testability altogether.
 */
object Interpolators {
    /*
     * ============================================================================================
     * Emphasized interpolators.
     * ============================================================================================
     */
    /**
     * The default emphasized interpolator. Used for hero / emphasized movement of content.
     */
    val EMPHASIZED: Interpolator = createEmphasizedInterpolator()

    /**
     * The accelerated emphasized interpolator. Used for hero / emphasized movement of content that
     * is disappearing e.g. when moving off screen.
     */
    val EMPHASIZED_ACCELERATE: Interpolator = PathInterpolator(
        0.3f, 0f, 0.8f, 0.15f
    )

    /**
     * The decelerating emphasized interpolator. Used for hero / emphasized movement of content that
     * is appearing e.g. when coming from off screen
     */
    val EMPHASIZED_DECELERATE: Interpolator = PathInterpolator(
        0.05f, 0.7f, 0.1f, 1f
    )
    /*
     * ============================================================================================
     * Standard interpolators.
     * ============================================================================================
     */
    /**
     * The standard interpolator that should be used on every normal animation
     */
    val STANDARD: Interpolator = PathInterpolator(
        0.2f, 0f, 0f, 1f
    )

    /**
     * The standard accelerating interpolator that should be used on every regular movement of
     * content that is disappearing e.g. when moving off screen.
     */
    val STANDARD_ACCELERATE: Interpolator = PathInterpolator(
        0.3f, 0f, 1f, 1f
    )

    /**
     * The standard decelerating interpolator that should be used on every regular movement of
     * content that is appearing e.g. when coming from off screen.
     */
    val STANDARD_DECELERATE: Interpolator = PathInterpolator(
        0f, 0f, 0f, 1f
    )
    /*
     * ============================================================================================
     * Legacy
     * ============================================================================================
     */
    /**
     * The default legacy interpolator as defined in Material 1. Also known as FAST_OUT_SLOW_IN.
     */
    val LEGACY: Interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    /**
     * The default legacy accelerating interpolator as defined in Material 1.
     * Also known as FAST_OUT_LINEAR_IN.
     */
    val LEGACY_ACCELERATE: Interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)

    /**
     * The default legacy decelerating interpolator as defined in Material 1.
     * Also known as LINEAR_OUT_SLOW_IN.
     */
    val LEGACY_DECELERATE: Interpolator = PathInterpolator(0f, 0f, 0.2f, 1f)

    /**
     * Linear interpolator. Often used if the interpolator is for different properties who need
     * different interpolations.
     */
    val LINEAR: Interpolator = LinearInterpolator()

    /*
     * ============================================================================================
     * Custom interpolators
     * ============================================================================================
     */
    val FAST_OUT_SLOW_IN = LEGACY
    val FAST_OUT_LINEAR_IN = LEGACY_ACCELERATE
    val LINEAR_OUT_SLOW_IN = LEGACY_DECELERATE

    /**
     * Like [.FAST_OUT_SLOW_IN], but used in case the animation is played in reverse (i.e. t
     * goes from 1 to 0 instead of 0 to 1).
     */
    val FAST_OUT_SLOW_IN_REVERSE: Interpolator = PathInterpolator(0.8f, 0f, 0.6f, 1f)
    val SLOW_OUT_LINEAR_IN: Interpolator = PathInterpolator(0.8f, 0f, 1f, 1f)
    val ALPHA_IN: Interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
    val ALPHA_OUT: Interpolator = PathInterpolator(0f, 0f, 0.8f, 1f)
    val ACCELERATE: Interpolator = AccelerateInterpolator()
    val ACCELERATE_DECELERATE: Interpolator = AccelerateDecelerateInterpolator()
    val DECELERATE_QUINT: Interpolator = DecelerateInterpolator(2.5f)
    val CUSTOM_40_40: Interpolator = PathInterpolator(0.4f, 0f, 0.6f, 1f)
    val ICON_OVERSHOT: Interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1.4f)
    val ICON_OVERSHOT_LESS: Interpolator = PathInterpolator(
        0.4f, 0f, 0.2f,
        1.1f
    )
    val PANEL_CLOSE_ACCELERATED: Interpolator = PathInterpolator(
        0.3f, 0f, 0.5f,
        1f
    )
    val BOUNCE: Interpolator = BounceInterpolator()

    /**
     * For state transitions on the control panel that lives in GlobalActions.
     */
    val CONTROL_STATE: Interpolator = PathInterpolator(
        0.4f, 0f, 0.2f,
        1.0f
    )

    /**
     * Interpolator to be used when animating a move based on a click. Pair with enough duration.
     */
    val TOUCH_RESPONSE: Interpolator = PathInterpolator(0.3f, 0f, 0.1f, 1f)

    /**
     * Like [.TOUCH_RESPONSE], but used in case the animation is played in reverse (i.e. t
     * goes from 1 to 0 instead of 0 to 1).
     */
    val TOUCH_RESPONSE_REVERSE: Interpolator = PathInterpolator(0.9f, 0f, 0.7f, 1f)
    /*
     * ============================================================================================
     * Functions / Utilities
     * ============================================================================================
     */
    /**
     * Calculate the amount of overshoot using an exponential falloff function with desired
     * properties, where the overshoot smoothly transitions at the 1.0f boundary into the
     * overshoot, retaining its acceleration.
     *
     * @param progress a progress value going from 0 to 1
     * @param overshootAmount the amount > 0 of overshoot desired. A value of 0.1 means the max
     * value of the overall progress will be at 1.1.
     * @param overshootStart the point in (0,1] where the result should reach 1
     * @return the interpolated overshoot
     */
    fun getOvershootInterpolation(
        progress: Float, overshootAmount: Float,
        overshootStart: Float
    ): Float {
        require(!(overshootAmount == 0.0f || overshootStart == 0.0f)) { "Invalid values for overshoot" }
        val b =
            (ln(((overshootAmount + 1) / overshootAmount).toDouble()) / overshootStart).toFloat()
        return 0.0f.coerceAtLeast((1.0f - Math.exp((-b * progress).toDouble())).toFloat() * (overshootAmount + 1.0f))
    }

    /**
     * Similar to [.getOvershootInterpolation] but the overshoot
     * starts immediately here, instead of first having a section of non-overshooting
     *
     * @param progress a progress value going from 0 to 1
     */
    fun getOvershootInterpolation(progress: Float): Float {
        return 0.0f.coerceAtLeast((1.0f - exp((-4 * progress).toDouble())).toFloat())
    }

    // Create the default emphasized interpolator
    private fun createEmphasizedInterpolator(): PathInterpolator {
        val path = Path()
        // Doing the same as fast_out_extra_slow_in
        path.moveTo(0f, 0f)
        path.cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        path.cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        return PathInterpolator(path)
    }
}
