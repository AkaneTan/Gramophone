package org.akanework.gramophone.ui.adapters

import android.content.Context
import androidx.appcompat.widget.PopupMenu
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils

class DateDecorAdapter(
    context: Context,
    dateCount: Int,
    dateAdapter: DateAdapter,
) : BaseDecorAdapter<DateAdapter, MediaStoreUtils.Date>(context, dateCount, dateAdapter) {
    override val pluralStr = R.plurals.items
}
