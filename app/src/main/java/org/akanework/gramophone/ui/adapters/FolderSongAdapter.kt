package org.akanework.gramophone.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R

class FolderSongAdapter(songList: MutableList<MediaItem>,
                        mainActivity: MainActivity
) : SongAdapter(songList, mainActivity) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_folder_song, parent, false),
        )
}