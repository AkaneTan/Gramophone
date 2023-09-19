package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

@androidx.annotation.OptIn(UnstableApi::class)
class AlbumAdapter(
    albumList: MutableList<MediaStoreUtils.Album>,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : BaseAdapter.ItemAdapter<MediaStoreUtils.Album>(R.layout.adapter_grid_card, albumList) {

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.title.text = list[position].title
        holder.subTitle.text = list[position].artist

        Glide
            .with(holder.songCover.context)
            .load(
                list[position]
                    .songList
                    .first()
                    .mediaMetadata
                    .artworkUri,
            ).placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)

        holder.itemView.setOnClickListener {
            fragmentManager
                .beginTransaction()
                .addToBackStack("SUBFRAG")
                .replace(
                    R.id.container,
                    GeneralSubFragment().apply {
                        arguments =
                            Bundle().apply {
                                putInt("Position", toRawPos(position))
                                putInt("Item", 1)
                                putString("Title", list[position].title)
                            }
                    },
                ).commit()
        }

        holder.moreButton.setOnClickListener {
            val popupMenu = PopupMenu(it.context, it)
            popupMenu.inflate(R.menu.more_menu_less)

            popupMenu.setOnMenuItemClickListener { it1 ->
                when (it1.itemId) {
                    R.id.play_next -> {
                        val mediaController = mainActivity.getPlayer()
                        mediaController.addMediaItems(
                            mediaController.currentMediaItemIndex + 1,
                            list[holder.bindingAdapterPosition].songList,
                        )
                    }

                    R.id.details -> {
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
            popupMenu.show()
        }
    }


}
