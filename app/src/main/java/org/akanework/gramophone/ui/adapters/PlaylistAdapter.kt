package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
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
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val mainActivity: MainActivity,
) : BaseAdapter.ItemAdapter<MediaStoreUtils.Playlist>(R.layout.adapter_list_card_larger, playlistList) {

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.title.text =
            list[position].title.ifEmpty {
                context.getString(R.string.unknown_playlist)
            }
        val songText =
            list[position].songList.size.toString() + ' ' +
                if (list[position].songList.size <= 1) {
                    context.getString(R.string.song)
                } else {
                    context.getString(R.string.songs)
                }
        holder.subTitle.text = songText

        if (list[position].songList.isNotEmpty()) {
            Glide
                .with(holder.songCover.context)
                .load(
                    list[position]
                        .songList
                        .first()
                        .mediaMetadata
                        .artworkUri,
                ).placeholder(R.drawable.ic_default_cover_playlist)
                .into(holder.songCover)
        } else {
            holder.songCover.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_default_cover_playlist))
        }

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
                                putInt("Item", 6)
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
                }
                true
            }
            popupMenu.show()
        }
    }

    override fun isPinned(item: MediaStoreUtils.Playlist): Boolean {
        return item.virtual
    }

}
