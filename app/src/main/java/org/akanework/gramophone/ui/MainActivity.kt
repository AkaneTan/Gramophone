package org.akanework.gramophone.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.SavedStateViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.checkEssentialPermission
import org.akanework.gramophone.logic.enableEdgeToEdgeProperly
import org.akanework.gramophone.logic.utils.AnimationUtils
import org.akanework.gramophone.logic.utils.BottomSheetUtils
import org.akanework.gramophone.logic.utils.BottomSheetUtils.BottomFrameManipulateState
import org.akanework.gramophone.ui.components.BottomSheet
import org.akanework.gramophone.ui.viewmodels.GramophoneViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bottomSheet: BottomSheet

    private var bottomSheetHeight = 0
    private var bottomNavigationHeight = 0

    private lateinit var btnShowNav: MaterialButton
    private lateinit var btnHideNav: MaterialButton
    private lateinit var btnShowSheet: MaterialButton
    private lateinit var btnHideSheet: MaterialButton
    private lateinit var btnShowAll: MaterialButton
    private lateinit var btnHideAll: MaterialButton

    private var bottomInset: Int = 0

    private val viewModel: GramophoneViewModel by viewModels {
        SavedStateViewModelFactory(application, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkEssentialPermission()) {
            val intent = Intent(this, SetupActivity::class.java)
            this.startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdgeProperly()
        setContentView(R.layout.activity_main)

        bottomSheetHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_height)
        bottomNavigationHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)

        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomSheet = findViewById(R.id.standard_bottom_sheet)
        btnShowNav = findViewById(R.id.show_nav)
        btnHideNav = findViewById(R.id.hide_nav)
        btnShowSheet = findViewById(R.id.show_sheet)
        btnHideSheet = findViewById(R.id.hide_sheet)
        btnShowAll = findViewById(R.id.show_all)
        btnHideAll = findViewById(R.id.hide_all)

        btnShowNav.setOnClickListener {
            manipulateBottomFrame(BottomFrameManipulateState.SHOW_NAV)
        }
        btnHideNav.setOnClickListener {
            manipulateBottomFrame(BottomFrameManipulateState.HIDE_NAV)
        }
        btnShowSheet.setOnClickListener {
            manipulateBottomFrame(BottomFrameManipulateState.SHOW_SHEET)
        }
        btnHideSheet.setOnClickListener {
            manipulateBottomFrame(BottomFrameManipulateState.HIDE_SHEET)
        }
        btnShowAll.setOnClickListener {
            manipulateBottomFrame(BottomFrameManipulateState.SHOW_ALL)
        }
        btnHideAll.setOnClickListener {
            manipulateBottomFrame(BottomFrameManipulateState.HIDE_ALL)
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            bottomInset = insets.bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.bottomNavTranslation = bottomNavigationView.translationY
        viewModel.bottomSheetPeekHeight = bottomSheet.peekHeight
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.translationY = viewModel.bottomNavTranslation
        bottomSheet.peekHeight = viewModel.bottomSheetPeekHeight
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bottomNavigationView.doOnLayout {
            bottomSheet.setBottomNavInvokeMethod {
                if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.SHOWN) {
                    bottomNavigationView.translationY = it
                }
            }
        }
    }

    private fun manipulateBottomFrame(state: BottomFrameManipulateState) {
        when (state) {
            BottomFrameManipulateState.HIDE_SHEET -> {
                manipulateBottomSheet(true)
                viewModel.bottomSheetState = BottomSheetUtils.ComponentState.HIDDEN
            }

            BottomFrameManipulateState.SHOW_SHEET -> {
                manipulateBottomSheet(false)
                viewModel.bottomSheetState = BottomSheetUtils.ComponentState.SHOWN
            }

            BottomFrameManipulateState.HIDE_NAV -> {
                manipulateNavigationView(true)
                viewModel.bottomNavState = BottomSheetUtils.ComponentState.HIDDEN
            }

            BottomFrameManipulateState.SHOW_NAV -> {
                manipulateNavigationView(false)
                viewModel.bottomNavState = BottomSheetUtils.ComponentState.SHOWN
            }

            BottomFrameManipulateState.HIDE_ALL -> {
                manipulateAll(true)
                viewModel.bottomSheetState = BottomSheetUtils.ComponentState.HIDDEN
                viewModel.bottomNavState = BottomSheetUtils.ComponentState.HIDDEN
            }

            BottomFrameManipulateState.SHOW_ALL -> {
                manipulateAll(false)
                viewModel.bottomSheetState = BottomSheetUtils.ComponentState.SHOWN
                viewModel.bottomNavState = BottomSheetUtils.ComponentState.SHOWN
            }
        }
    }

    private fun manipulateBottomSheet(hide: Boolean = false) {
        val isBottomNavShown = viewModel.bottomNavState == BottomSheetUtils.ComponentState.SHOWN
        val isBottomSheetShown = viewModel.bottomSheetState == BottomSheetUtils.ComponentState.SHOWN
        val targetPeekHeight: Int
        val finalPeekHeight: Int

        if (hide) {
            if (isBottomSheetShown) {
                targetPeekHeight =
                    if (isBottomNavShown) bottomSheetHeight + bottomNavigationHeight + bottomInset else bottomInset + bottomSheetHeight
                finalPeekHeight = if (isBottomNavShown) bottomInset + bottomNavigationHeight else 0

                AnimationUtils.createValAnimator(
                    targetPeekHeight,
                    finalPeekHeight,
                    doOnEnd = {
                        if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.HIDDEN) {
                            bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }) {
                    bottomSheet.peekHeight = it
                }
            }
        } else {
            if (!isBottomSheetShown) {
                if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.HIDDEN ||
                    bottomSheet.state == BottomSheetBehavior.STATE_HIDDEN
                ) {
                    bottomSheet.setSlideInvokeMethod { }
                    bottomSheet.peekHeight = 0
                    bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                }

                targetPeekHeight = if (isBottomNavShown) bottomInset + bottomNavigationHeight else 0
                finalPeekHeight =
                    if (isBottomNavShown) bottomSheetHeight + bottomNavigationHeight + bottomInset else bottomInset + bottomSheetHeight

                AnimationUtils.createValAnimator(
                    targetPeekHeight,
                    finalPeekHeight
                ) {
                    bottomSheet.peekHeight = it
                }
            }
        }
    }


    private fun manipulateNavigationView(hide: Boolean = false) {
        if ((hide && bottomNavigationView.translationY == 0F) ||
            (!hide && bottomNavigationView.translationY != 0F)
        ) {
            var shouldAlsoManipulateBottomSheet = 0
            if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.SHOWN && viewModel.bottomSheetState == BottomSheetUtils.ComponentState.HIDDEN)
                shouldAlsoManipulateBottomSheet = 1
            if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.HIDDEN && viewModel.bottomSheetState == BottomSheetUtils.ComponentState.HIDDEN)
                shouldAlsoManipulateBottomSheet = 2
            bottomSheet.setSlideInvokeMethod { }
            if (shouldAlsoManipulateBottomSheet == 1) {
                bottomSheet.peekHeight = bottomInset + bottomSheetHeight
                bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                viewModel.bottomSheetState = BottomSheetUtils.ComponentState.HIDDEN
            }
            AnimationUtils.createValAnimator<Float>(
                if (hide) 0F else 1F,
                if (hide) 1F else 0F,
                doOnEnd = {
                    if (shouldAlsoManipulateBottomSheet == 2) {
                        bottomSheet.peekHeight = bottomInset + bottomSheetHeight
                        bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                        viewModel.bottomSheetState = BottomSheetUtils.ComponentState.HIDDEN
                    } else if (shouldAlsoManipulateBottomSheet == 0) {
                        bottomSheet.setSlideInvokeMethod(null)
                    }
                }
            ) {
                bottomNavigationView.translationY = (bottomNavigationHeight + bottomInset) * it
                if (shouldAlsoManipulateBottomSheet == 0) {
                    bottomSheet.peekHeight =
                        (bottomInset + bottomSheetHeight + bottomNavigationHeight * (1F - it)).toInt()
                }
            }
        }
    }

    private fun manipulateAll(hide: Boolean = false) {
        if (viewModel.bottomNavState != viewModel.bottomSheetState) {
            if (hide) {
                if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.SHOWN) manipulateBottomFrame(
                    BottomFrameManipulateState.HIDE_NAV
                )
                if (viewModel.bottomSheetState == BottomSheetUtils.ComponentState.SHOWN) manipulateBottomFrame(
                    BottomFrameManipulateState.HIDE_SHEET
                )
            } else {
                if (viewModel.bottomNavState == BottomSheetUtils.ComponentState.HIDDEN) manipulateBottomFrame(
                    BottomFrameManipulateState.SHOW_NAV
                )
                if (viewModel.bottomSheetState == BottomSheetUtils.ComponentState.HIDDEN) manipulateBottomFrame(
                    BottomFrameManipulateState.SHOW_SHEET
                )
            }
            return
        }
        bottomSheet.peekHeight = bottomNavigationHeight + bottomSheetHeight + bottomInset
        bottomSheet.state =
            if (hide) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED
        bottomSheet.setSlideInvokeMethod {
            bottomNavigationView.translationY = -it
        }
    }

    fun connectBottomNavigationView(listener : OnItemSelectedListener) {
        bottomNavigationView.setOnItemSelectedListener(listener)
    }
}