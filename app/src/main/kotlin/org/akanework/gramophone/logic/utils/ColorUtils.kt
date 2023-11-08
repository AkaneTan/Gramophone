package org.akanework.gramophone.logic.utils

import androidx.core.graphics.ColorUtils
import kotlin.math.min

object ColorUtils {

    private const val DEFAULT_COLOR_BACKGROUND_ELEVATED_CHROMA = 1.2f
    private const val DEFAULT_COLOR_BACKGROUND_ELEVATED_LIGHTING = 0.99f
    private const val DEFAULT_COLOR_BACKGROUND_CHROMA = 0.9f
    private const val DEFAULT_COLOR_BACKGROUND_LIGHTING = 1.015f

    fun getColorBackgroundElevated(color: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        hsl[1] *= DEFAULT_COLOR_BACKGROUND_ELEVATED_CHROMA
        hsl[1] = min(hsl[1], 1f)
        hsl[2] *= DEFAULT_COLOR_BACKGROUND_ELEVATED_LIGHTING

        return ColorUtils.HSLToColor(hsl)
    }

    fun getColorBackground(color: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        hsl[1] *= DEFAULT_COLOR_BACKGROUND_CHROMA
        hsl[1] = min(hsl[1], 1f)
        hsl[2] *= DEFAULT_COLOR_BACKGROUND_LIGHTING

        return ColorUtils.HSLToColor(hsl)
    }

}