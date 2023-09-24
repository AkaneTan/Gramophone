package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

/**
 * [PlaylistAdapter] is an adapter for displaying artists.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaylistAdapter(
    playlistList: MutableList<MediaStoreUtils.Playlist>,
    context: Context,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : BaseAdapter.ItemAdapter<MediaStoreUtils.Playlist>
    (context, playlistList) {

    override val layout = R.layout.adapter_list_card_larger
    override val defaultCover = R.drawable.ic_default_cover_playlist

    override fun titleOf(item: MediaStoreUtils.Playlist): String {
        if (item is MediaStoreUtils.RecentlyAdded) {
            return context.getString(R.string.recently_added)
        }
        return item.title ?: context.getString(R.string.unknown_playlist)
    }

    override fun onClick(item: MediaStoreUtils.Playlist) {
        fragmentManager
            .beginTransaction()
            .addToBackStack("SUBFRAG")
            .replace(
                R.id.container,
                GeneralSubFragment().apply {
                    arguments =
                        Bundle().apply {
                            putInt("Position", toRawPos(item))
                            putInt("Item", 6)
                            putString("Title", titleOf(item))
                        }
                },
            ).commit()
    }

    override fun onMenu(item: MediaStoreUtils.Playlist, popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.more_menu_less)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = mainActivity.getPlayer()
                    mediaController.addMediaItems(
                        mediaController.currentMediaItemIndex + 1,
                        item.songList,
                    )
                }

                R.id.details -> {
                    // TODO
                }
            }
            true
        }
    }

    override fun isPinned(item: MediaStoreUtils.Playlist): Boolean {
        return item.virtual
    }

}
