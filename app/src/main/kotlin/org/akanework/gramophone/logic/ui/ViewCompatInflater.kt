package org.akanework.gramophone.logic.ui

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.graphics.TypefaceCompat
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.theme.MaterialComponentsViewInflater
import org.akanework.gramophone.R

@Suppress("UNUSED") // reflection by androidx via theme attr viewInflaterClass
class ViewCompatInflater
@JvmOverloads constructor(impl: ViewCompatInflaterImpl = ViewCompatInflaterImpl())
	: CallbackViewInflater(impl, impl) {
	class ViewCompatInflaterImpl : MaterialComponentsViewInflater(), Callback {

		override fun createTextView(context: Context, attrs: AttributeSet?): AppCompatTextView {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
				return TypefaceCompatTextView(context, attrs)
			}
			return super.createTextView(context, attrs)
		}

		class TypefaceCompatTextView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
			MaterialTextView(context, attrs, defStyleAttr) {
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
			override fun setTextAppearance(context: Context, resId: Int) {
				super.setTextAppearance(context, resId)
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

		}

		override fun onCreateView(
			context: Context,
			name: String,
			attrs: AttributeSet,
			result: View?
		) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P && result is TextView) {
				// perhaps some descendant, not just TypefaceCompatTextView
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
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				val a = context.theme.obtainStyledAttributes(
					attrs,
					R.styleable.MyTooltipCompat,
					0,
					0
				)
				val tooltip = a.getText(R.styleable.MyTooltipCompat_android_tooltipText)
				if (tooltip != null) {
					result?.let { TooltipCompat.setTooltipText(it, tooltip) }
				}
			}
		}
	}
}