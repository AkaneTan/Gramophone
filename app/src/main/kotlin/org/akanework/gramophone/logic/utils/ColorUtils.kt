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
        hsl[2] *= DEFAULT_COLOR_BACKGROUND_LIGHTING

        return ColorUtils.HSLToColor(hsl)
    }

    fun getColorToolbarElevated(color: Int, context: Context): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        hsl[1] *= DEFAULT_COLOR_TOOLBAR_ELEVATED_CHROMA
        val nightModeFlags: Int = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                hsl[2] *= DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING_DARK
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                hsl[2] *= DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                hsl[2] *= DEFAULT_COLOR_TOOLBAR_ELEVATED_LIGHTING
            }
        }

        return ColorUtils.HSLToColor(hsl)
    }

}