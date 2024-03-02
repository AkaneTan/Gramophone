package org.akanework.gramophone.ui.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.BaseAdapter

class GridPaddingDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private var mPadding = context.resources.getDimensionPixelSize(R.dimen.grid_card_side_padding)
    private val columnSize = if (context.resources.configuration.orientation
        == Configuration.ORIENTATION_PORTRAIT) 2 else 4

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.adapter !is ConcatAdapter
            || ((parent.adapter as ConcatAdapter).adapters[1] !is BaseAdapter<*>
            && (parent.adapter as ConcatAdapter).adapters[0] !is ConcatAdapter)) {
            throw IllegalArgumentException("Can't find desired adapter!")
        }
        if (parent.adapter is ConcatAdapter
            && (parent.adapter as ConcatAdapter).adapters[1] is BaseAdapter<*>
            && ((parent.adapter as ConcatAdapter).adapters[1] as BaseAdapter<*>).layoutType
            != BaseAdapter.LayoutType.GRID
        ) {
            throw IllegalArgumentException("Target adapter layout type is not GridLayout!")
        }

        if ((parent.adapter as ConcatAdapter).adapters[1] !is BaseAdapter<*>
                    && (parent.adapter as ConcatAdapter).adapters[0] is ConcatAdapter) {
            ((parent.adapter as ConcatAdapter).adapters[0] as ConcatAdapter).adapters[1].let {
                val itemPosition = parent.getChildAdapterPosition(view)
                if (itemPosition > it.itemCount) {
                    return@getItemOffsets
                }
                if (itemPosition == RecyclerView.NO_POSITION) {
                    return@getItemOffsets
                } else if (itemPosition % columnSize == 0 && itemPosition != 0) {
                    outRect.right = mPadding
                } else if (itemPosition % columnSize - 1 == 0) {
                    outRect.left = mPadding
                } else {
                    return@getItemOffsets
                }
            }
        } else {
            (parent.adapter as ConcatAdapter).adapters[1]?.let {
                val itemPosition = parent.getChildAdapterPosition(view)
                if (itemPosition == RecyclerView.NO_POSITION) {
                    return@getItemOffsets
                } else if (itemPosition % columnSize == 0 && itemPosition != 0) {
                    outRect.right = mPadding
                } else if (itemPosition % columnSize - 1 == 0) {
                    outRect.left = mPadding
                } else {
                    return@getItemOffsets
                }
            }
        }
    }
}