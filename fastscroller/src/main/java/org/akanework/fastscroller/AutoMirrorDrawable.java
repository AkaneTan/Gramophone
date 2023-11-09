/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.akanework.fastscroller;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat;
import androidx.core.graphics.drawable.DrawableCompat;

@SuppressLint("RestrictedApi")
class AutoMirrorDrawable extends DrawableWrapperCompat {

    public AutoMirrorDrawable(@NonNull Drawable drawable) {
        super(drawable);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (needMirroring()) {
            float centerX = getBounds().exactCenterX();
            canvas.scale(-1, 1, centerX, 0);
            super.draw(canvas);
            canvas.scale(-1, 1, centerX, 0);
        } else {
            super.draw(canvas);
        }
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection) {
        super.onLayoutDirectionChanged(layoutDirection);
        return true;
    }

    @Override
    public boolean isAutoMirrored() {
        return true;
    }

    private boolean needMirroring() {
        return DrawableCompat.getLayoutDirection(this) == View.LAYOUT_DIRECTION_RTL;
    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
        boolean hasPadding = super.getPadding(padding);
        if (needMirroring()) {
            int paddingStart = padding.left;
            int paddingEnd = padding.right;
            padding.left = paddingEnd;
            padding.right = paddingStart;
        }
        return hasPadding;
    }
}
