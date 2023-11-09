/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package org.akanework.fastscroller;

import android.graphics.Canvas;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SimpleViewHelper implements FastScroller.ViewHelper {

    @Nullable
    private Runnable mOnPreDrawListener;

    @Nullable
    private Runnable mOnScrollChangedListener;

    @Nullable
    private Predicate<MotionEvent> mOnTouchEventListener;
    private boolean mListenerInterceptingTouchEvent;

    @Override
    public void addOnPreDrawListener(@Nullable Runnable listener) {
        mOnPreDrawListener = listener;
    }

    public void draw(@NonNull Canvas canvas) {

        if (mOnPreDrawListener != null) {
            mOnPreDrawListener.run();
        }

        superDraw(canvas);
    }

    @Override
    public void addOnScrollChangedListener(@Nullable Runnable listener) {
        mOnScrollChangedListener = listener;
    }

    public void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        superOnScrollChanged(left, top, oldLeft, oldTop);

        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.run();
        }
    }

    @Override
    public void addOnTouchEventListener(@Nullable Predicate<MotionEvent> listener) {
        mOnTouchEventListener = listener;
    }

    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {

        if (mOnTouchEventListener != null && mOnTouchEventListener.test(event)) {

            int actionMasked = event.getActionMasked();
            if (actionMasked != MotionEvent.ACTION_UP
                    && actionMasked != MotionEvent.ACTION_CANCEL) {
                mListenerInterceptingTouchEvent = true;
            }

            if (actionMasked != MotionEvent.ACTION_CANCEL) {
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                superOnInterceptTouchEvent(cancelEvent);
                cancelEvent.recycle();
            } else {
                superOnInterceptTouchEvent(event);
            }

            return true;
        }

        return superOnInterceptTouchEvent(event);
    }

    public boolean onTouchEvent(@NonNull MotionEvent event) {

        if (mOnTouchEventListener != null) {
            if (mListenerInterceptingTouchEvent) {

                mOnTouchEventListener.test(event);

                int actionMasked = event.getActionMasked();
                if (actionMasked == MotionEvent.ACTION_UP
                        || actionMasked == MotionEvent.ACTION_CANCEL) {
                    mListenerInterceptingTouchEvent = false;
                }

                return true;
            } else {
                int actionMasked = event.getActionMasked();
                if (actionMasked != MotionEvent.ACTION_DOWN && mOnTouchEventListener.test(event)) {

                    if (actionMasked != MotionEvent.ACTION_UP
                            && actionMasked != MotionEvent.ACTION_CANCEL) {
                        mListenerInterceptingTouchEvent = true;
                    }

                    if (actionMasked != MotionEvent.ACTION_CANCEL) {
                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        superOnTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    } else {
                        superOnTouchEvent(event);
                    }

                    return true;
                }
            }
        }

        return superOnTouchEvent(event);
    }

    @Override
    public int getScrollRange() {
        return computeVerticalScrollRange();
    }

    @Override
    public int getScrollOffset() {
        return computeVerticalScrollOffset();
    }

    @Override
    public void scrollTo(int offset) {
        scrollTo(getScrollX(), offset);
    }

    protected abstract void superDraw(@NonNull Canvas canvas);

    protected abstract void superOnScrollChanged(int left, int top, int oldLeft, int oldTop);

    protected abstract boolean superOnInterceptTouchEvent(@NonNull MotionEvent event);

    protected abstract boolean superOnTouchEvent(@NonNull MotionEvent event);

    protected abstract int computeVerticalScrollRange();

    protected abstract int computeVerticalScrollOffset();

    protected abstract int getScrollX();

    protected abstract void scrollTo(int x, int y);
}
