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

@androidx.annotation.OptIn(UnstableApi::class)
class AlbumAdapter(
    albumList: MutableList<MediaStoreUtils.Album>,
    private val fragmentManager: FragmentManager,
    context: Context,
) : BaseAdapter.ItemAdapter<MediaStoreUtils.Album>
    (context, albumList) {

    override val layout = R.layout.adapter_grid_card

    override fun titleOf(item: MediaStoreUtils.Album): String {
        return item.title ?: context.getString(R.string.unknown_album)
    }

    override fun subTitleOf(item: MediaStoreUtils.Album): String {
        return item.artist ?: context.getString(R.string.unknown_artist)
    }

    override fun onClick(item: MediaStoreUtils.Album) {
        fragmentManager
            .beginTransaction()
            .addToBackStack("SUBFRAG")
            .replace(
                R.id.container,
                GeneralSubFragment().apply {
                    arguments =
                        Bundle().apply {
                            putInt("Position", toRawPos(item))
                            putInt("Item", 1)
                            putString("Title", item.title)
                        }
                },
            ).commit()
    }

    override fun onMenu(item: MediaStoreUtils.Album, popupMenu: PopupMenu) {
        popupMenu.inflate(R.menu.more_menu_less)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.play_next -> {
                    val mediaController = (context as MainActivity).getPlayer()
                    mediaController.addMediaItems(
                        mediaController.currentMediaItemIndex + 1,
                        item.songList,
                    )
                }

                R.id.details -> {
                    // TODO
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
            }
            true
        }
    }

    override fun isPinned(item: MediaStoreUtils.Album): Boolean {
        return item.title == null
    }
}
