package org.akanework.gramophone.logic.utils;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;

public class ColorUtils {

    /**
     * Set the alpha component of {@code color} to be {@code alpha}.
     */
    @ColorInt
    public static int setAlphaComponent(@ColorInt int color,
                                        @IntRange(from = 0x0, to = 0xFF) int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("alpha must be between 0 and 255.");
        }
        return (color & 0x00ffffff) | (alpha << 24);
    }

}
