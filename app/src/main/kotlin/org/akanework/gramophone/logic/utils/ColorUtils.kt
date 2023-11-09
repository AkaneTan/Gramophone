package org.akanework.gramophone.logic.utils

import android.content.Context
import android.content.res.Configuration
import androidx.core.graphics.ColorUtils
import kotlin.math.min

object ColorUtils {

    private const val DEFAULT_COLOR_BACKGROUND_ELEVATED_CHROMA = 1.2f
    private const val DEFAULT_COLOR_BACKGROUND_ELEVATED_LIGHTING = 0.99f
    private const val DEFAULT_COLOR_BACKGROUND_CHROMA = 0.9f
    private const val DEFAULT_COLOR_BACKGROUND_LIGHTING = 1.015f
    private const val DEFAULT_COLOR_TOOLBAR_ELEVATED_CHROMA = 1.2f
    private const val DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING = 0.97f
    private const val DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING_DARK = 1.5f

    private fun manipulateHsl(color: Int, chroma: Float, lighting: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        hsl[1] *= chroma
        hsl[1] = min(hsl[1], 1f)
        hsl[2] *= lighting
        hsl[2] = min(hsl[2], 1f)

        return ColorUtils.HSLToColor(hsl)
    }

    fun getColorBackgroundElevated(color: Int): Int =
        manipulateHsl(
            color,
            DEFAULT_COLOR_BACKGROUND_ELEVATED_CHROMA,
            DEFAULT_COLOR_BACKGROUND_ELEVATED_LIGHTING
        )

    fun getColorBackground(color: Int): Int =
        manipulateHsl(
            color,
            DEFAULT_COLOR_BACKGROUND_CHROMA,
            DEFAULT_COLOR_BACKGROUND_LIGHTING
        )

    fun getColorToolbarElevated(color: Int, context: Context): Int {
        val nightModeFlags: Int = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            manipulateHsl(
                color,
                DEFAULT_COLOR_TOOLBAR_ELEVATED_CHROMA,
                DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING_DARK
            )
        } else {
            manipulateHsl(
                color,
                DEFAULT_COLOR_TOOLBAR_ELEVATED_CHROMA,
                DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING
            )
        }
    }

}