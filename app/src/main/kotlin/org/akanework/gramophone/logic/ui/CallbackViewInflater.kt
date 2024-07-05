/**
 * This file (CallbackViewInflater.kt) is dual-licensed (you can choose one):
 * - Project license (as of writing GPL-3.0-or-later)
 * - Apache-2.0
 * Because it's really only Android boilerplate hacks, and I hope this will be of use for someone.
 * Copyright (C) 2024 nift4
 */

package org.akanework.gramophone.logic.ui

import android.content.Context
import android.util.AttributeSet
import android.view.InflateException
import android.view.View
import androidx.appcompat.app.AppCompatViewInflater
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.collection.SimpleArrayMap
import java.lang.reflect.Constructor

/**
 * AppCompatViewInflater that delegates actual implementation to another AppCompatViewInflater but
 * also adds a Callback which is run for EVERY view created. Great to backport things.
 */
abstract class CallbackViewInflater(private val parent: AppCompatViewInflater, private val cb: Callback)
	: AppCompatViewInflater() {
	// copied from AppCompatViewInflater.class
	private val sClassPrefixList = arrayOf("android.widget.", "android.view.", "android.webkit.")

	final override fun createView(context: Context, name: String, attrs: AttributeSet): View? {
		return (parent.createView(null, name, context, attrs,
			false, false, false, false)
			?: viewFromTag(context, name, attrs))
			.also {
				cb.onCreateView(context, attrs, it)
			}
	}

	private fun viewFromTag(context: Context, name: String, attrs: AttributeSet): View? {
		try {
			mConstructorArgs[0] = context
			mConstructorArgs[1] = attrs
			var view = createViewByPrefix(context, name, null)
			if (view != null) return view
			for (i in sClassPrefixList) {
				view = createViewByPrefix(context, name, i)
				if (view != null) return view
			}
		} finally {
			mConstructorArgs[0] = null
			mConstructorArgs[1] = null
		}
		return null
	}

	final override fun createAutoCompleteTextView(
		context: Context, attrs: AttributeSet
	): AppCompatAutoCompleteTextView {
		return createView(context, "AutoCompleteTextView", attrs) as AppCompatAutoCompleteTextView
	}

	final override fun createButton(context: Context, attrs: AttributeSet): AppCompatButton {
		return createView(context, "Button", attrs) as AppCompatButton
	}

	final override fun createCheckBox(context: Context, attrs: AttributeSet): AppCompatCheckBox {
		return createView(context, "CheckBox", attrs) as AppCompatCheckBox
	}

	final override fun createCheckedTextView(
		context: Context,
		attrs: AttributeSet
	): AppCompatCheckedTextView {
		return createView(context, "CheckedTextView", attrs) as AppCompatCheckedTextView
	}

	final override fun createEditText(context: Context, attrs: AttributeSet): AppCompatEditText {
		return createView(context, "EditText", attrs) as AppCompatEditText
	}

	final override fun createImageButton(context: Context, attrs: AttributeSet): AppCompatImageButton {
		return createView(context, "ImageButton", attrs) as AppCompatImageButton
	}

	final override fun createImageView(context: Context, attrs: AttributeSet): AppCompatImageView {
		return createView(context, "ImageView", attrs) as AppCompatImageView
	}

	final override fun createMultiAutoCompleteTextView(
		context: Context,
		attrs: AttributeSet
	): AppCompatMultiAutoCompleteTextView {
		return createView(context, "MultiAutoCompleteTextView", attrs) as AppCompatMultiAutoCompleteTextView
	}

	final override fun createRadioButton(context: Context, attrs: AttributeSet): AppCompatRadioButton {
		return createView(context, "RadioButton", attrs) as AppCompatRadioButton
	}

	final override fun createRatingBar(context: Context, attrs: AttributeSet): AppCompatRatingBar {
		return createView(context, "RatingBar", attrs) as AppCompatRatingBar
	}

	final override fun createSeekBar(context: Context, attrs: AttributeSet): AppCompatSeekBar {
		return createView(context, "SeekBar", attrs) as AppCompatSeekBar
	}

	final override fun createSpinner(context: Context, attrs: AttributeSet): AppCompatSpinner {
		return createView(context, "Spinner", attrs) as AppCompatSpinner
	}

	final override fun createTextView(context: Context, attrs: AttributeSet): AppCompatTextView {
		return createView(context, "TextView", attrs) as AppCompatTextView
	}

	final override fun createToggleButton(
		context: Context, attrs: AttributeSet
	): AppCompatToggleButton {
		return createView(context, "ToggleButton", attrs) as AppCompatToggleButton
	}

	interface Callback {
		fun onCreateView(context: Context, attrs: AttributeSet, result: View?)
	}

	private val sConstructorMap: SimpleArrayMap<String?, Constructor<out View?>?> =
		SimpleArrayMap()
	private val sConstructorSignature = arrayOf(
		Context::class.java,
		AttributeSet::class.java
	)
	private val mConstructorArgs = arrayOfNulls<Any>(2)

	@Throws(ClassNotFoundException::class, InflateException::class)
	private fun createViewByPrefix(context: Context, name: String, prefix: String?): View? {
		var constructor: Constructor<out View>? = sConstructorMap[name]

		try {
			if (constructor == null) {
				val clazz = Class.forName(
					if (prefix != null) prefix + name else name,
					false,
					context.classLoader
				).asSubclass(
					View::class.java
				)
				constructor = clazz.getConstructor(*sConstructorSignature)
				sConstructorMap.put(name, constructor)
			}

			constructor!!.isAccessible = true
			return constructor.newInstance(*this.mConstructorArgs)
		} catch (ex: Exception) {
			return null
		}
	}
}