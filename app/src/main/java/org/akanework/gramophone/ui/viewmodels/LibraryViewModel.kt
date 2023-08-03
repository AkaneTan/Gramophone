package org.akanework.gramophone.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import org.akanework.gramophone.logic.utils.MediaStoreUtils

class LibraryViewModel : ViewModel() {
    val mediaItemList: MutableLiveData<MutableList<MediaItem>> = MutableLiveData(mutableListOf())
    val albumItemList: MutableLiveData<MutableList<MediaStoreUtils.Album>> = MutableLiveData(mutableListOf())
    val artistItemList: MutableLiveData<MutableList<MediaStoreUtils.Artist>> = MutableLiveData(
        mutableListOf()
    )
    val genreItemList: MutableLiveData<MutableList<MediaStoreUtils.Genre>> = MutableLiveData(
        mutableListOf()
    )
    val dateItemList: MutableLiveData<MutableList<MediaStoreUtils.Date>> = MutableLiveData(
        mutableListOf()
    )
}