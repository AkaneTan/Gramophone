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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.widget.NestedScrollView;
import androidx.fluidrecyclerview.widget.RecyclerView;

public class FastScrollerBuilder {

    @NonNull
    private final ViewGroup mView;

    @Nullable
    private FastScroller.ViewHelper mViewHelper;

    @Nullable
    private PopupTextProvider mPopupTextProvider;

    @Nullable
    private Rect mPadding;

    @NonNull
    private Drawable mTrackDrawable;

    @NonNull
    private Drawable mThumbDrawable;

    @NonNull
    private Consumer<TextView> mPopupStyle;

    @Nullable
    private FastScroller.AnimationHelper mAnimationHelper;

    public FastScrollerBuilder(@NonNull ViewGroup view) {
        mView = view;
        useDefaultStyle();
    }

    @NonNull
    public FastScrollerBuilder setViewHelper(@Nullable FastScroller.ViewHelper viewHelper) {
        mViewHelper = viewHelper;
        return this;
    }

    @NonNull
    public FastScrollerBuilder setPopupTextProvider(@Nullable PopupTextProvider popupTextProvider) {
        mPopupTextProvider = popupTextProvider;
        return this;
    }

    @NonNull
    public FastScrollerBuilder setPadding(int left, int top, int right, int bottom) {
        if (mPadding == null) {
            mPadding = new Rect();
        }
        mPadding.set(left, top, right, bottom);
        return this;
    }

    @NonNull
    public FastScrollerBuilder setPadding(@Nullable Rect padding) {
        if (padding != null) {
            if (mPadding == null) {
                mPadding = new Rect();
            }
            mPadding.set(padding);
        } else {
            mPadding = null;
        }
        return this;
    }

    @NonNull
    public FastScrollerBuilder setTrackDrawable(@NonNull Drawable trackDrawable) {
        mTrackDrawable = trackDrawable;
        return this;
    }

    @NonNull
    public FastScrollerBuilder setThumbDrawable(@NonNull Drawable thumbDrawable) {
        mThumbDrawable = thumbDrawable;
        return this;
    }

    @NonNull
    public FastScrollerBuilder setPopupStyle(@NonNull Consumer<TextView> popupStyle) {
        mPopupStyle = popupStyle;
        return this;
    }

    @NonNull
    public FastScrollerBuilder useDefaultStyle() {
        Context context = mView.getContext();
        mTrackDrawable = Utils.getGradientDrawableWithTintAttr(R.drawable.afs_track,
                com.google.android.material.R.attr.colorControlNormal, context);
        mThumbDrawable = Utils.getGradientDrawableWithTintAttr(R.drawable.afs_thumb,
                com.google.android.material.R.attr.colorControlActivated, context);
        mPopupStyle = PopupStyles.DEFAULT;
        return this;
    }

    @NonNull
    public FastScrollerBuilder useMd2Style() {
        Context context = mView.getContext();
        mTrackDrawable = Utils.getGradientDrawableWithTintAttr(R.drawable.afs_md2_track,
                com.google.android.material.R.attr.colorControlNormal, context);
        mThumbDrawable = Utils.getGradientDrawableWithTintAttr(R.drawable.afs_md2_thumb,
                com.google.android.material.R.attr.colorControlActivated, context);
        mPopupStyle = PopupStyles.MD2;
        return this;
    }

    public void setAnimationHelper(@Nullable FastScroller.AnimationHelper animationHelper) {
        mAnimationHelper = animationHelper;
    }

    public void disableScrollbarAutoHide() {
        DefaultAnimationHelper animationHelper = new DefaultAnimationHelper(mView);
        animationHelper.setScrollbarAutoHideEnabled(false);
        mAnimationHelper = animationHelper;
    }

    @NonNull
    public FastScroller build() {
        return new FastScroller(mView, getOrCreateViewHelper(), mPadding, mTrackDrawable,
                mThumbDrawable, mPopupStyle, getOrCreateAnimationHelper());
    }

    @NonNull
    private FastScroller.ViewHelper getOrCreateViewHelper() {
        if (mViewHelper != null) {
            return mViewHelper;
        }
        if (mView instanceof ViewHelperProvider) {
            return ((ViewHelperProvider) mView).getViewHelper();
        } else if (mView instanceof RecyclerView) {
            return new RecyclerViewHelper((RecyclerView) mView, mPopupTextProvider);
        } else if (mView instanceof NestedScrollView) {
            throw new UnsupportedOperationException("Please use "
                    + FastScrollNestedScrollView.class.getSimpleName() + " instead of "
                    + NestedScrollView.class.getSimpleName() + "for fast scroll");
        } else if (mView instanceof ScrollView) {
            throw new UnsupportedOperationException("Please use "
                    + FastScrollScrollView.class.getSimpleName() + " instead of "
                    + ScrollView.class.getSimpleName() + "for fast scroll");
        } else if (mView instanceof WebView) {
            throw new UnsupportedOperationException("Please use "
                    + FastScrollWebView.class.getSimpleName() + " instead of "
                    + WebView.class.getSimpleName() + "for fast scroll");
        } else {
            throw new UnsupportedOperationException(mView.getClass().getSimpleName()
                    + " is not supported for fast scroll");
        }
    }

    @NonNull
    private FastScroller.AnimationHelper getOrCreateAnimationHelper() {
        if (mAnimationHelper != null) {
            return mAnimationHelper;
        }
        return new DefaultAnimationHelper(mView);
    }
}
