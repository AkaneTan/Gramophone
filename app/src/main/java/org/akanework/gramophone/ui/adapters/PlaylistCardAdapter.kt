package org.akanework.gramophone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.FolderBrowserFragment

class PlaylistCardAdapter(private val playlist: MutableList<MediaItem>,
                          private val instance: MediaController)
    : RecyclerView.Adapter<PlaylistCardAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistCardAdapter.ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_list_card_smaller, parent, false),
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songName.text = playlist[holder.bindingAdapterPosition].mediaMetadata.title
        holder.songArtist.text = playlist[holder.bindingAdapterPosition].mediaMetadata.artist
        Glide
            .with(holder.songCover.context)
            .load(playlist[position].mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)
        holder.closeButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            playlist.removeAt(pos)
            notifyItemRemoved(pos)
            instance.removeMediaItem(pos)
        }
    }

    override fun getItemCount(): Int = playlist.size

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val songName: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
        val songCover: ImageView = view.findViewById(R.id.cover)
        val closeButton: MaterialButton = view.findViewById(R.id.close)
    }

}