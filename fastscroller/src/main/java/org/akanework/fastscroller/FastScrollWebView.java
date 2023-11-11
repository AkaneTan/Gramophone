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
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

@SuppressLint("MissingSuperCall")
public class FastScrollWebView extends WebView implements ViewHelperProvider {

    @NonNull
    private final ViewHelper mViewHelper = new ViewHelper();

    public FastScrollWebView(@NonNull Context context) {
        super(context);

        init();
    }

    public FastScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public FastScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs,
                             @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    public FastScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs,
                             @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }

    private void init() {
        setVerticalScrollBarEnabled(false);
        setScrollContainer(true);
    }

    @NonNull
    @Override
    public FastScroller.ViewHelper getViewHelper() {
        return mViewHelper;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        mViewHelper.draw(canvas);
    }

    @Override
    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        mViewHelper.onScrollChanged(left, top, oldLeft, oldTop);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        return mViewHelper.onInterceptTouchEvent(event);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return mViewHelper.onTouchEvent(event);
    }

    private class ViewHelper extends SimpleViewHelper {

        @Override
        protected void superDraw(@NonNull Canvas canvas) {
            FastScrollWebView.super.draw(canvas);
        }

        @Override
        protected void superOnScrollChanged(int left, int top, int oldLeft, int oldTop) {
            FastScrollWebView.super.onScrollChanged(left, top, oldLeft, oldTop);
        }

        @Override
        protected boolean superOnInterceptTouchEvent(@NonNull MotionEvent event) {
            return FastScrollWebView.super.onInterceptTouchEvent(event);
        }

        @Override
        protected boolean superOnTouchEvent(@NonNull MotionEvent event) {
            return FastScrollWebView.super.onTouchEvent(event);
        }

        @Override
        protected int computeVerticalScrollRange() {
            return FastScrollWebView.this.computeVerticalScrollRange();
        }

        @Override
        protected int computeVerticalScrollOffset() {
            return FastScrollWebView.this.computeVerticalScrollOffset();
        }

        @Override
        protected int getScrollX() {
            return FastScrollWebView.this.getScrollX();
        }

        @Override
        protected void scrollTo(int x, int y) {
            FastScrollWebView.this.scrollTo(x, y);
        }
    }
}
