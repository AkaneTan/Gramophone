/*
 * Copyright (C) 2009 The Android Open Source Project
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
package org.akanework.gramophone.logic.utils;

/**
 * A class that contains utility methods related to numbers.
 *
 * @hide Pending API council approval
 */
public final class MathUtils {
    private static final float DEG_TO_RAD = 3.1415926f / 180.0f;
    private static final float RAD_TO_DEG = 180.0f / 3.1415926f;

    private MathUtils() {
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : (Math.min(amount, high));
    }


    public static float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    public static float lerp(int start, int stop, float amount) {
        return lerp((float) start, (float) stop, amount);
    }

    /**
     * Returns the interpolation scalar (s) that satisfies the equation: {@code value = }{@link
     * #lerp}{@code (a, b, s)}
     *
     * <p>If {@code a == b}, then this function will return 0.
     */
    public static float lerpInv(float a, float b, float value) {
        return a != b ? ((value - a) / (b - a)) : 0.0f;
    }

    /** Returns the single argument constrained between [0.0, 1.0]. */
    public static float saturate(float value) {
        return constrain(value, 0.0f, 1.0f);
    }

    /** Returns the saturated (constrained between [0, 1]) result of {@link #lerpInv}. */
    public static float lerpInvSat(float a, float b, float value) {
        return saturate(lerpInv(a, b, value));
    }

}
