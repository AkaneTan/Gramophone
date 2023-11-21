package org.akanework.gramophone.logic.utils

import android.content.Context
import android.content.res.Configuration
import androidx.core.graphics.ColorUtils
import kotlin.math.min

object ColorUtils {
    var overrideAmoledColor = false

    enum class ColorType(
        var chroma: Float = 0f,
        var lighting: Float = 0f,
        var lightingDark: Float = 0f
    ) {
        COLOR_BACKGROUND_ELEVATED(1.2f, 0.99f, 0.99f),
        COLOR_BACKGROUND(0.9f, 1.015f, 1.015f),
        TOOLBAR_ELEVATED(0.6f, 0.97f, 1.5f),
        COLOR_PRIMARY_FAINTED(0.4f, 0.8f, 0.8f)
    }

    private fun manipulateHsl(
        color: Int,
        colorType: ColorType,
        context: Context,
        overrideAmoledColor: Boolean = false
    ): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        hsl[1] *= colorType.chroma
        hsl[1] = min(hsl[1], 1f)

        if (isDarkMode(context)) {
            hsl[2] *= colorType.lightingDark
            hsl[2] = min(hsl[2], 1f)
            if (overrideAmoledColor) {
                hsl[2] = 0f
            }
        } else {
            hsl[2] *= colorType.lighting
            hsl[2] = min(hsl[2], 1f)
        }

        return ColorUtils.HSLToColor(hsl)
    }

    fun getColor(color: Int, colorType: ColorType, context: Context): Int =
        manipulateHsl(color, colorType, context)

    private fun isDarkMode(context: Context): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

}