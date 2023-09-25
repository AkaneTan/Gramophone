package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SupportComparator

class AlbumDecorAdapter(
    context: Context,
    albumCount: Int,
    albumAdapter: AlbumAdapter,
) : BaseDecorAdapter<AlbumAdapter, MediaStoreUtils.Album>(context, albumCount, albumAdapter) {
    override val pluralStr = R.plurals.albums
}
