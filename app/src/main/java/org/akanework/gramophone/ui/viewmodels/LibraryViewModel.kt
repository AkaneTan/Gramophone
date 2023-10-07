package org.akanework.gramophone.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import org.akanework.gramophone.logic.utils.MediaStoreUtils

class LibraryViewModel : ViewModel() {
    val mediaItemList: MutableLiveData<MutableList<MediaItem>> = MutableLiveData(mutableListOf())
    val albumItemList: MutableLiveData<MutableList<MediaStoreUtils.Album>> =
        MutableLiveData(mutableListOf())
    val albumArtistItemList: MutableLiveData<MutableList<MediaStoreUtils.Artist>> =
        MutableLiveData(
            mutableListOf(),
        )
    val artistItemList: MutableLiveData<MutableList<MediaStoreUtils.Artist>> =
        MutableLiveData(
            mutableListOf(),
        )
    val genreItemList: MutableLiveData<MutableList<MediaStoreUtils.Genre>> =
        MutableLiveData(
            mutableListOf(),
        )
    val dateItemList: MutableLiveData<MutableList<MediaStoreUtils.Date>> =
        MutableLiveData(
            mutableListOf(),
        )
    val playlistList: MutableLiveData<MutableList<MediaStoreUtils.Playlist>> =
        MutableLiveData(
            mutableListOf(),
        )
    val folderStructure: MutableLiveData<MediaStoreUtils.FileNode> =
        MutableLiveData(
            MediaStoreUtils.FileNode("storage", mutableListOf(), mutableListOf())
        )
}
