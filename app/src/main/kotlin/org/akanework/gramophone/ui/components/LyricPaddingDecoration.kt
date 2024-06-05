package org.akanework.gramophone.ui.components

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R

class LyricPaddingDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val topPadding = context.resources.getDimensionPixelSize(R.dimen.lyric_top_padding)
    private val bottomPadding =
        context.resources.getDimensionPixelSize(R.dimen.lyric_bottom_padding)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemPosition = parent.getChildAdapterPosition(view)
        if (itemPosition == 0) {
            outRect.top = topPadding
        } else if (itemPosition == parent.adapter!!.itemCount - 1) {
            outRect.bottom = bottomPadding
        }
    }
}