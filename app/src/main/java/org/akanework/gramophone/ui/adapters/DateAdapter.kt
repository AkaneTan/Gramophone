package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

/**
 * [DateAdapter] is an adapter for displaying artists.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class DateAdapter(
    dateList: MutableList<MediaStoreUtils.Date>,
    context: Context,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : BaseAdapter.ItemAdapter<MediaStoreUtils.Date>
    (context, dateList, Sorter.from()) {

    override val layout = R.layout.adapter_list_card_larger
    override val defaultCover = R.drawable.ic_default_cover_date

    override fun titleOf(item: MediaStoreUtils.Date): String {
        return item.title ?: context.getString(R.string.unknown_year)
    }

    override fun onClick(item: MediaStoreUtils.Date) {
        fragmentManager
            .beginTransaction()
            .addToBackStack("SUBFRAG")
            .replace(
                R.id.container,
                GeneralSubFragment().apply {
                    arguments =
                        Bundle().apply {
                            putInt("Position", toRawPos(item))
                            putInt("Item", 4)
                            putString("Title", titleOf(item))
                        }
                },
            ).commit()
    }

    override fun onMenu(item: MediaStoreUtils.Date, popupMenu: PopupMenu) {
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

    override fun isPinned(item: MediaStoreUtils.Date): Boolean {
        return item.title == null
    }

}
