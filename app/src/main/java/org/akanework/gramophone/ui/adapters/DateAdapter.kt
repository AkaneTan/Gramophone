package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.fragments.GeneralSubFragment

/**
 * [DateAdapter] is an adapter for displaying artists.
 */
class DateAdapter(private val dateList: MutableList<MediaStoreUtils.Date>,
                  private val context: Context,
                  private val fragmentManager: FragmentManager) :
    RecyclerView.Adapter<DateAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_list_card_larger, parent, false))
    }

    override fun getItemCount(): Int = dateList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = if (dateList[position].title == 0)
            context.getString(R.string.unknown_year)
        else
            dateList[position].title.toString()
        val songText = dateList[position].songList.size.toString() + ' ' +
                if (dateList[position].songList.size <= 1)
                    context.getString(R.string.song)
                else
                    context.getString(R.string.songs)
        holder.songArtist.text = songText

        Glide.with(holder.songCover.context)
            .load(dateList[position].songList.first().mediaMetadata.artworkUri)
            .placeholder(R.drawable.ic_default_cover_date)
            .into(holder.songCover)

        holder.itemView.setOnClickListener {
            fragmentManager.beginTransaction()
                .addToBackStack("SUBFRAG")
                .replace(R.id.container, GeneralSubFragment().apply {
                    arguments = Bundle().apply {
                        putInt("Position", position)
                        putInt("Item", 4)
                        putString("Title", holder.songTitle.text as String)
                    }
                })
                .commit()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songCover: ImageView = view.findViewById(R.id.cover)
        val songTitle: TextView = view.findViewById(R.id.title)
        val songArtist: TextView = view.findViewById(R.id.artist)
    }

    fun sortByDescending(selector: (MediaStoreUtils.Date) -> String) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasDateList = mutableListOf<MediaStoreUtils.Date>()
            wasDateList.addAll(dateList)
            // Sorting in the background using coroutines
            dateList.sortByDescending { selector(it) }

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasDateList, dateList))
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@DateAdapter)
            }
        }
    }

    fun sortByDescendingInt(selector: (MediaStoreUtils.Date) -> Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val wasDateList = mutableListOf<MediaStoreUtils.Date>()
            wasDateList.addAll(dateList)
            // Sorting in the background using coroutines
            dateList.sortByDescending { selector(it) }

            val diffResult = DiffUtil.calculateDiff(SongDiffCallback(wasDateList, dateList))
            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                diffResult.dispatchUpdatesTo(this@DateAdapter)
            }
        }
    }

    fun updateList(newList: MutableList<MediaStoreUtils.Date>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(dateList, newList))
        dateList.clear()
        dateList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(private val oldList: MutableList<MediaStoreUtils.Date>, private val newList: MutableList<MediaStoreUtils.Date>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].id == newList[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}