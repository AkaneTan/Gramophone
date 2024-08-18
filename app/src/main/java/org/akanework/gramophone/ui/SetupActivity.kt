package org.akanework.gramophone.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginLeft
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.checkEssentialPermission
import org.akanework.gramophone.logic.enableEdgeToEdgeProperly
import org.akanework.gramophone.logic.setCurrentItemInterpolated
import org.akanework.gramophone.logic.utils.AnimationUtils
import org.akanework.gramophone.ui.adapters.SetupViewPagerAdapter

class SetupActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SetupViewPagerAdapter
    private lateinit var continueMaterialButton: MaterialButton
    private lateinit var previousMaterialButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdgeProperly()
        setContentView(R.layout.activity_setup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager2)
        continueMaterialButton = findViewById(R.id.next_btn)
        previousMaterialButton = findViewById(R.id.prev_btn)

        adapter = SetupViewPagerAdapter(supportFragmentManager, lifecycle)

        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 9999

        continueMaterialButton.setOnClickListener {
            if (viewPager.currentItem + 1 < adapter.itemCount) {
                viewPager.setCurrentItemInterpolated(viewPager.currentItem + 1, duration = AnimationUtils.MID_DURATION)
                isPreviousMaterialButtonAvailable = viewPager.currentItem + 1 != 0
                isContinueMaterialButtonEnabled = this.checkEssentialPermission() && viewPager.currentItem + 1 == 1
            }
        }

        previousMaterialButton.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.setCurrentItemInterpolated(viewPager.currentItem - 1, duration = AnimationUtils.MID_DURATION)
                isPreviousMaterialButtonAvailable = viewPager.currentItem - 1 != 0
                isContinueMaterialButtonEnabled = !(!this.checkEssentialPermission() && viewPager.currentItem - 1 == 1)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("TAG", "viewPager.c: ${viewPager.currentItem}")
        if (viewPager.currentItem != 0) {
            isPreviousMaterialButtonAvailable = true
        }
        isContinueMaterialButtonEnabled = !(!this.checkEssentialPermission() && viewPager.currentItem == 1)
    }

    var isPreviousMaterialButtonAvailable: Boolean
        get() = previousMaterialButton.marginLeft == 0
        set(value) {
            if ((value && previousMaterialButton.marginLeft == 0) ||
                (!value && previousMaterialButton.marginLeft != 0)) return
            AnimationUtils.createValAnimator<Int>(
                if (value) -resources.getDimensionPixelSize(R.dimen.button_width) else 0,
                if (value) 0 else -resources.getDimensionPixelSize(R.dimen.button_width),
                AnimationUtils.MID_DURATION
            ) {
                previousMaterialButton.updateLayoutParams<MarginLayoutParams> {
                    leftMargin = it
                }
            }
        }

    var isContinueMaterialButtonEnabled: Boolean
        get() = continueMaterialButton.isEnabled
        set(value) {
            if (continueMaterialButton.isEnabled == value) return
            val disabledContainerTint = MaterialColors.getColor(continueMaterialButton, com.google.android.material.R.attr.colorOutlineVariant)
            val disabledIconTint = MaterialColors.getColor(continueMaterialButton, com.google.android.material.R.attr.colorOutline)
            val enabledContainerTint = MaterialColors.getColor(continueMaterialButton, com.google.android.material.R.attr.colorPrimary)
            val enabledIconTint = MaterialColors.getColor(continueMaterialButton, com.google.android.material.R.attr.colorOnPrimary)
            continueMaterialButton.isEnabled = value
            AnimationUtils.createValAnimator<Int>(
                if (value) disabledContainerTint else enabledContainerTint,
                if (value) enabledContainerTint else disabledContainerTint,
                isArgb = true
            ) {
                continueMaterialButton.backgroundTintList = ColorStateList.valueOf(
                    it
                )
            }
            AnimationUtils.createValAnimator<Int>(
                if (value) disabledIconTint else enabledIconTint,
                if (value) enabledIconTint else disabledIconTint,
                isArgb = true
            ) {
                continueMaterialButton.iconTint = ColorStateList.valueOf(
                    it
                )
            }
        }

}