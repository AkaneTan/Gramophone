package org.akanework.gramophone.logic.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.logic.dpToPx

class CustomLinearLayoutManager(private val context: Context, attrs: AttributeSet?,
                                defStyleAttr: Int, defStyleRes: Int)
    : LinearLayoutManager(context, attrs, defStyleAttr, defStyleRes) {
        constructor(context: Context) : this(context, null, 0, 0)
    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        extraLayoutSpace[0] = 128.dpToPx(context)
        extraLayoutSpace[1] = 128.dpToPx(context)
    }
}