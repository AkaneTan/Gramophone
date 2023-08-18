package org.akanework.gramophone.ui.adapters

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R

/**
 * [SongAdapter] is an adapter for displaying songs.
 */
@UnstableApi class SongAdapter(private val songList: MutableList<MediaItem>,
                  private val mainActivity: MainActivity) :
    RecyclerView.Adapter<SongAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_list_card, parent, false))
    }

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = songList[position].mediaMetadata.title
        holder.songArtist.text = songList[position].mediaMetadata.artist

        Glide.with(holder.songCover.context)
            .load(songList[position].mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover)
            .into(holder.songCover)

        holder.itemView.setOnClickListener {
            val standardBottomSheet = mainActivity.findViewById<FrameLayout>(R.id.player_layout)
            val standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet)
            val mediaController = mainActivity.getPlayer()
            mediaController.setMediaItems(songList)
            mediaController.seekToDefaultPosition(holder.bindingAdapterPosition)
            mediaController.prepare()
            mediaController.play()
            if (standardBottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                Handler(Looper.getMainLooper()).postDelayed({
                    standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                    if (standardBottomSheetBehavior.isHideable) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            standardBottomSheetBehavior.isHideable = false
                        }, 300)
                    }
                }, 200)
            }
        }

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
    }

    fun sortBy(selector: (MediaItem) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasSongList = mutableListOf<MediaItem>()
            wasSongList.addAll(songList)
            // Sorting in the background using coroutines
            songList.sortBy { selector(it) }

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasSongList, songList))
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@SongAdapter)
            }
        }
    }

    fun updateList(newList: MutableList<MediaItem>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(songList, newList))
        songList.clear()
        songList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(private val oldList: List<MediaItem>, private val newList: List<MediaItem>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].mediaId == newList[newItemPosition].mediaId
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}