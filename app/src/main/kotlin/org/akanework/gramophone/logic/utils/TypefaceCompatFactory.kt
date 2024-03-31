/*
 * Copyright (C) 2015 fountaingeyser
 *               2024 Akane Foundation
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

package org.akanework.gramophone.logic.utils

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.LayoutInflater.Factory2
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.LayoutInflaterCompat
import org.akanework.gramophone.R

/**
 * This class magically backports android:fontWeight attribute. Based on similar
 * (but for a different purpose) TypefaceCompatFactory:
 * https://github.com/taltstidl/AppCompat-Extension-Library/blob/f5933bec02340e84e82c6bf56d0d2c3c13628c45/appcompat-extension/src/main/java/com/tr4android/support/extension/typeface/TypefaceCompatFactory.java#L46
 */

class TypefaceCompatFactory private constructor(context: Context?) : Factory2 {
	private val mBaseFactory: Factory2?

	init {
		mBaseFactory = try {
			(context as AppCompatActivity).delegate as Factory2
		} catch (e: ClassCastException) {
			e.printStackTrace(); null
		}
	}

	override fun onCreateView(
		parent: View?,
		name: String,
		context: Context,
		attrs: AttributeSet
	): View? {
		var result: View? = null
		if (name == "TextView") {
			result = TypefaceCompatTextView(context, attrs)
		} else if (mBaseFactory != null) {
			result = mBaseFactory.onCreateView(parent, name, context, attrs)
		}
		if (result is TextView) { // perhaps some descendant, not just TypefaceCompatTextView
			var fontWeight = -1
			val theme = context.theme
			var a = theme.obtainStyledAttributes(
				attrs,
				androidx.appcompat.R.styleable.AppCompatTextView,
				0,
				0
			)
			var appearance: TypedArray? = null
			val ap = a.getResourceId(
				androidx.appcompat.R.styleable.AppCompatTextView_android_textAppearance,
				-1
			)
			a.recycle()
			if (ap != -1) {
				appearance = theme.obtainStyledAttributes(ap, R.styleable.MyTextAppearance)
			}
			if (appearance != null) {
				fontWeight =
					appearance.getInt(R.styleable.MyTextAppearance_textFontWeight, -1)
				appearance.recycle()
			}
			a = theme.obtainStyledAttributes(
				attrs,
				androidx.appcompat.R.styleable.TextAppearance,
				0,
				0
			)
			if (a.hasValue(androidx.appcompat.R.styleable.TextAppearance_android_textFontWeight)) {
				fontWeight = a.getInt(
					androidx.appcompat.R.styleable.TextAppearance_android_textFontWeight,
					-1
				)
			}
			a.recycle()
			if (fontWeight != -1) {
				val tf = TypefaceCompat.create(
					result.context,
					result.typeface,
					fontWeight,
					result.typeface.isItalic
				)
				result.setTypeface(tf)
			}
		}
		return result
	}

	override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
		return onCreateView(null, name, context, attrs)
	}

	companion object {
		// call before super.onCreate() in AppCompatActivity!
		fun installViewFactory(context: Context?) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
				LayoutInflaterCompat.setFactory2(
					LayoutInflater.from(context),
					TypefaceCompatFactory(context)
				)
			}
		}
	}

	class TypefaceCompatTextView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
		AppCompatTextView(context, attrs, defStyleAttr) {
		constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
		constructor(context: Context) : this(context, null)

		override fun setTextAppearance(resId: Int) {
			super.setTextAppearance(resId)
			val a = if (resId != -1) {
				context.theme.obtainStyledAttributes(resId, R.styleable.MyTextAppearance)
			} else null
			val fontWeight = a?.getInt(R.styleable.MyTextAppearance_textFontWeight, -1)?.also {
				a.recycle()
			} ?: -1
			if (fontWeight != -1) {
				val tf = TypefaceCompat.create(context, typeface, fontWeight, typeface.isItalic)
				setTypeface(tf)
			}
		}

		@Deprecated(
			"Deprecated in Java", ReplaceWith(
				"super.setTextAppearance(resId)",
				"android.widget.TextView"
			)
		)
		override fun setTextAppearance(context: Context?, resId: Int) {
			super.setTextAppearance(context, resId)
			val ctx = context ?: this.context
			val a = if (resId != -1) {
				ctx.theme.obtainStyledAttributes(resId, R.styleable.MyTextAppearance)
			} else null
			val fontWeight = a?.getInt(R.styleable.MyTextAppearance_textFontWeight, -1)?.also {
				a.recycle()
			} ?: -1
			if (fontWeight != -1) {
				val tf = TypefaceCompat.create(ctx, typeface, fontWeight, typeface.isItalic)
				setTypeface(tf)
			}
		}

	}
}