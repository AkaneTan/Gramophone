package org.akanework.gramophone.ui.adapters

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [ArtistAdapter] is an adapter for displaying artists.
 */
class ArtistAdapter(
    private val mainActivity: MainActivity,
    artistList: MutableList<MediaStoreUtils.Artist>,
) : ItemAdapter<MediaStoreUtils.Artist>
    (mainActivity, artistList, Sorter.from()) {

    private var isAlbumArtist = false
    override val layout = R.layout.adapter_list_card_larger
    override val defaultCover = R.drawable.ic_default_cover_artist

    override fun titleOf(item: MediaStoreUtils.Artist): String {
        return item.title ?: context.getString(R.string.unknown_artist)
    }

    override fun onClick(item: MediaStoreUtils.Artist) {
        mainActivity.supportFragmentManager
            .beginTransaction()
            .addToBackStack("SUBFRAG")
            .add(
                R.id.container,
                GeneralSubFragment().apply {
                    arguments =
                        Bundle().apply {
                            putInt("Position", toRawPos(item))
                            putInt("Item",
                                if (!isAlbumArtist)
                                    2
                                else
                                    5)
                            putString("Title", titleOf(item))
                        }
                },
            ).commit()
    }

    override fun onMenu(item: MediaStoreUtils.Artist, popupMenu: PopupMenu) {

        popupMenu.inflate(R.menu.more_menu_less)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = mainActivity.getPlayer()
                    mediaController.addMediaItems(
                        mediaController.currentMediaItemIndex + 1,
                        item.songList,
                    )
                    true
                }

                /*
				R.id.share -> {
					val builder = ShareCompat.IntentBuilder(mainActivity)
					val mimeTypes = mutableSetOf<String>()
					builder.addStream(viewModel.fileUriList.value?.get(songList[holder.bindingAdapterPosition].mediaId.toLong())!!)
					mimeTypes.add(viewModel.mimeTypeList.value?.get(songList[holder.bindingAdapterPosition].mediaId.toLong())!!)
					builder.setType(mimeTypes.singleOrNull() ?: "audio/*").startChooser()
				 } */
				 */
                else -> false
            }
        }
    }

    override fun isPinned(item: MediaStoreUtils.Artist): Boolean {
        return item.title == null
    }

    fun setClickEventToAlbumArtist(reverse: Boolean) {
        isAlbumArtist = !reverse
    }
}

class ArtistDecorAdapter(
    artistAdapter: ArtistAdapter,
    private val prefs: SharedPreferences
) : BaseDecorAdapter<ArtistAdapter>(artistAdapter, R.plurals.artists) {
    private val viewModel: LibraryViewModel by (context as MainActivity).viewModels()

    override fun onSortButtonPressed(popupMenu: PopupMenu) {
        popupMenu.menu.findItem(R.id.album_artist).isVisible = true
        popupMenu.menu.findItem(R.id.album_artist).isChecked =
            prefs.getBoolean("isDisplayingAlbumArtist", false)
    }

    override fun onExtraMenuButtonPressed(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.album_artist -> {
                menuItem.isChecked = !menuItem.isChecked
                if (!prefs.getBoolean("isDisplayingAlbumArtist", false)) {
                    prefs.edit().putBoolean("isDisplayingAlbumArtist", true).apply()
                    var itemCount = 0
                    viewModel.albumArtistItemList.value?.let { it1 ->
                        adapter.updateList(
                            it1
                        )
                        itemCount = it1.size
                    }
                    updateSongCounter(itemCount)
                    adapter.setClickEventToAlbumArtist(false)
                } else {
                    prefs.edit().putBoolean("isDisplayingAlbumArtist", false).apply()
                    var itemCount = 0
                    menuItem.isChecked = !menuItem.isChecked
                    viewModel.artistItemList.value?.let { it1 ->
                        adapter.updateList(
                            it1
                        )
                        itemCount = it1.size
                    }
                    updateSongCounter(itemCount)
                    adapter.setClickEventToAlbumArtist(true)
                }
                true
            }

            else -> false
        }
    }

    fun updateListToAlbumArtist() {
        prefs.edit().putBoolean("isDisplayingAlbumArtist", true).apply()
        var itemCount = 0
        viewModel.albumArtistItemList.value?.let { it1 ->
            adapter.updateList(
                it1
            )
            itemCount = it1.size
        }
        updateSongCounter(itemCount)
        adapter.setClickEventToAlbumArtist(false)
    }
}