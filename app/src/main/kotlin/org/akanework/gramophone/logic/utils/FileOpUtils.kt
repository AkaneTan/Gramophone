package org.akanework.gramophone.logic.utils

import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.DateAdapter
import org.akanework.gramophone.ui.adapters.GenreAdapter
import org.akanework.gramophone.ui.adapters.PlaylistAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import java.lang.IllegalArgumentException

object FileOpUtils {
    fun getAdapterType(adapter: BaseAdapter<*>) =
        when (adapter) {
            is AlbumAdapter -> {
                0
            }
            is ArtistAdapter -> {
                1
            }
            is DateAdapter -> {
                2
            }
            is GenreAdapter -> {
                3
            }
            is PlaylistAdapter -> {
                4
            }
            is SongAdapter -> {
                5
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
}