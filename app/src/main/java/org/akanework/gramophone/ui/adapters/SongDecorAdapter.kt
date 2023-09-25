package org.akanework.gramophone.ui.adapters

import android.content.Context
import androidx.appcompat.widget.PopupMenu
import androidx.media3.common.MediaItem
import org.akanework.gramophone.R

class SongDecorAdapter(
    context: Context,
    songCount: Int,
    songAdapter: SongAdapter,
) : BaseDecorAdapter<SongAdapter, MediaItem>
    (context, songCount, songAdapter) {
    override val pluralStr = R.plurals.songs
}
