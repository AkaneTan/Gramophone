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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fluidrecyclerview.widget.RecyclerView;

public class FixItemDecorationRecyclerView extends RecyclerView {

    public FixItemDecorationRecyclerView(@NonNull Context context) {
        super(context);
    }

    public FixItemDecorationRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixItemDecorationRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
                                         @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        for (int i = 0, count = getItemDecorationCount(); i < count; ++i) {
            FixItemDecoration decor = (FixItemDecoration) super.getItemDecorationAt(i);
            decor.getItemDecoration().onDraw(canvas, this, decor.getState());
        }
        super.dispatchDraw(canvas);
        for (int i = 0, count = getItemDecorationCount(); i < count; ++i) {
            FixItemDecoration decor = (FixItemDecoration) super.getItemDecorationAt(i);
            decor.getItemDecoration().onDrawOver(canvas, this, decor.getState());
        }
    }

    @Override
    public void addItemDecoration(@NonNull ItemDecoration decor, int index) {
        super.addItemDecoration(new FixItemDecoration(decor), index);
    }

    @NonNull
    @Override
    public ItemDecoration getItemDecorationAt(int index) {
        return ((FixItemDecoration) super.getItemDecorationAt(index)).getItemDecoration();
    }

    @Override
    public void removeItemDecoration(@NonNull ItemDecoration decor) {
        if (!(decor instanceof FixItemDecoration)) {
            for (int i = 0, count = getItemDecorationCount(); i < count; ++i) {
                FixItemDecoration fixDecor = (FixItemDecoration) super.getItemDecorationAt(i);
                if (fixDecor.getItemDecoration() == decor) {
                    decor = fixDecor;
                    break;
                }
            }
        }
        super.removeItemDecoration(decor);
    }

    private static class FixItemDecoration extends ItemDecoration {

        @NonNull
        private final ItemDecoration mItemDecoration;

        private State mState;

        private FixItemDecoration(@NonNull ItemDecoration itemDecoration) {
            mItemDecoration = itemDecoration;
        }

        @NonNull
        public ItemDecoration getItemDecoration() {
            return mItemDecoration;
        }

        public State getState() {
            return mState;
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull State state) {
            mState = state;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent) {}

        @Override
        public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent,
                               @NonNull State state) {}

        @Override
        @SuppressWarnings("deprecation")
        public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent) {}

        @Override
        @SuppressWarnings("deprecation")
        public void getItemOffsets(@NonNull Rect outRect, int itemPosition,
                                   @NonNull RecyclerView parent) {
            mItemDecoration.getItemOffsets(outRect, itemPosition, parent);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull State state) {
            mItemDecoration.getItemOffsets(outRect, view, parent, state);
        }
    }
}
