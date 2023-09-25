package org.akanework.gramophone.ui.adapters

import android.content.Context
import androidx.appcompat.widget.PopupMenu
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils

class GenreDecorAdapter(
    context: Context,
    genreCount: Int,
    genreAdapter: GenreAdapter,
) : BaseDecorAdapter<GenreAdapter, MediaStoreUtils.Genre>(context, genreCount, genreAdapter) {
    override val pluralStr = R.plurals.items
}
