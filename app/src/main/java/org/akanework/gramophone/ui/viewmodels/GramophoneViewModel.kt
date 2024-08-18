package org.akanework.gramophone.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.akanework.gramophone.logic.utils.BottomSheetUtils.ComponentState

class GramophoneViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    companion object {
        const val BOTTOM_SHEET_STATE_TOKEN = "bottomSheetState"
        const val BOTTOM_SHEET_PEEK_HEIGHT_TOKEN = "bottomSheetPeekHeight"
        const val BOTTOM_NAV_STATE_TOKEN = "bottomNavState"
        const val BOTTOM_NAV_TRANSLATION_TOKEN = "bottomNavTrans"
    }
    var bottomSheetState: ComponentState
        get() = savedStateHandle[BOTTOM_SHEET_STATE_TOKEN] ?: ComponentState.HIDDEN
        set(value) {
            savedStateHandle[BOTTOM_SHEET_STATE_TOKEN] = value
        }
    var bottomNavState: ComponentState
        get() = savedStateHandle[BOTTOM_NAV_STATE_TOKEN] ?: ComponentState.SHOWN
        set(value) {
            savedStateHandle[BOTTOM_NAV_STATE_TOKEN] = value
        }
    var bottomNavTranslation: Float
        get() = savedStateHandle[BOTTOM_NAV_TRANSLATION_TOKEN] ?: 0f
        set(value) {
            savedStateHandle[BOTTOM_NAV_TRANSLATION_TOKEN] = value
        }
    var bottomSheetPeekHeight: Int
        get() = savedStateHandle[BOTTOM_SHEET_PEEK_HEIGHT_TOKEN] ?: 0
        set(value) {
            savedStateHandle[BOTTOM_SHEET_PEEK_HEIGHT_TOKEN] = value
        }
}