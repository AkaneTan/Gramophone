package org.akanework.gramophone.ui.adapters

import android.content.Context
import androidx.appcompat.widget.PopupMenu
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils

class PlaylistDecorAdapter(
    context: Context,
    count: Int,
    adapter: PlaylistAdapter,
) : BaseDecorAdapter<PlaylistAdapter, MediaStoreUtils.Playlist>
    (context, count, adapter) {
    override val pluralStr = R.plurals.playlists
}
