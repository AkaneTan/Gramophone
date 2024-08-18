package org.akanework.gramophone.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.CustomBottomSheetBehavior
import kotlin.math.max
import kotlin.math.min

class BottomSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var fullPlayer: FullPlayer
    private lateinit var previewPlayer: PreviewPlayer

    private var standardBottomSheetBehavior: CustomBottomSheetBehavior<FrameLayout>? = null
    private var _peekHeight: Int = 0

    private var slideInvoke: ((peekHeight: Float) -> Unit)? = null
    private var bottomNavInvoke: ((translationY: Float) -> Unit)? = null

    private val bottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    slideInvoke = null
                    previewPlayer.alpha = 1f
                    previewPlayer.visibility = View.VISIBLE
                    fullPlayer.visibility = View.GONE
                }

                BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                    previewPlayer.visibility = View.VISIBLE
                    fullPlayer.visibility = View.VISIBLE
                }

                BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    previewPlayer.visibility = View.GONE
                    fullPlayer.visibility = View.VISIBLE
                    previewPlayer.alpha = 0f
                }

                BottomSheetBehavior.STATE_HIDDEN -> {
                    slideInvoke = null
                    fullPlayer.visibility = View.GONE
                    previewPlayer.visibility = View.GONE
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            slideInvoke?.let {
                slideInvoke?.invoke(
                    (if (standardBottomSheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED)
                        _peekHeight
                    else
                        measuredHeight
                            ) * slideOffset
                )
            }
            if (slideInvoke == null) {
                val fullPlayerAlpha = measuredHeight * slideOffset / previewPlayer.measuredHeight
                bottomNavInvoke?.invoke(measuredHeight * slideOffset)
                fullPlayer.alpha = min(fullPlayerAlpha, 1f)
                previewPlayer.alpha =
                    max((1f - fullPlayerAlpha), 0f)
            }
        }
    }

    init {
        inflate(context, R.layout.layout_bottom_sheet, this)
        fullPlayer = findViewById(R.id.full_player)
        previewPlayer = findViewById(R.id.preview_player)

        this.setBackgroundColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurfaceContainerLow
            )
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnLayout {
            standardBottomSheetBehavior = CustomBottomSheetBehavior.from(this)

            standardBottomSheetBehavior!!.addBottomSheetCallback(bottomSheetCallback)

            when (standardBottomSheetBehavior!!.state) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    previewPlayer.alpha = 0f
                    previewPlayer.visibility = View.GONE
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    fullPlayer.alpha = 0f
                    fullPlayer.visibility = View.GONE
                }
                else -> { }
            }

            if (_peekHeight != 0) {
                standardBottomSheetBehavior!!.peekHeight = _peekHeight
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        standardBottomSheetBehavior!!.removeBottomSheetCallback(bottomSheetCallback)
        standardBottomSheetBehavior = null
    }

    var peekHeight: Int
        get() = standardBottomSheetBehavior?.peekHeight ?: _peekHeight
        set(value) {
            this._peekHeight = value
            standardBottomSheetBehavior?.peekHeight = value
        }

    var state: Int?
        get() = standardBottomSheetBehavior?.state
        set(value) {
            value?.let { standardBottomSheetBehavior?.state = value }
        }

    fun setSlideInvokeMethod(method: ((peekHeight: Float) -> Unit)?) {
        slideInvoke = method
    }

    fun setBottomNavInvokeMethod(method: ((translationY: Float) -> Unit)?) {
        bottomNavInvoke = method
    }
}