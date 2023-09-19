package org.akanework.gramophone.ui.adapters

import android.content.Context
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
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : BaseAdapter.ItemAdapter<MediaStoreUtils.Date>(R.layout.adapter_list_card_larger, dateList) {

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.title.text =
            if (list[position].title == "0") {
                context.getString(R.string.unknown_year)
            } else {
                list[position].title
            }
        val songText =
            list[position].songList.size.toString() + ' ' +
                if (list[position].songList.size <= 1) {
                    context.getString(R.string.song)
                } else {
                    context.getString(R.string.songs)
                }
        holder.subTitle.text = songText

        Glide
            .with(holder.songCover.context)
            .load(
                list[position]
                    .songList
                    .first()
                    .mediaMetadata
                    .artworkUri,
            ).placeholder(R.drawable.ic_default_cover_date)
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
                                putInt("Item", 4)
                                putString("Title", holder.title.text as String)
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
