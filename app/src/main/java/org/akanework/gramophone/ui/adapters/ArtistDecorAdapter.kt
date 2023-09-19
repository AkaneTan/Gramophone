package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.SupportComparator
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

class ArtistDecorAdapter(
    private val context: Context,
    private var artistCount: Int,
    private val artistAdapter: ArtistAdapter,
    mainActivity: MainActivity,
    private val prefs: SharedPreferences
) : RecyclerView.Adapter<ArtistDecorAdapter.ViewHolder>() {
    private var sortStatus = 0
    private val viewModel: LibraryViewModel by mainActivity.viewModels()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.general_decor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val songText =
            artistCount.toString() + ' ' +
                if (artistCount <= 1) context.getString(R.string.artist) else context.getString(R.string.artists)
        holder.songCounter.text = songText
        holder.sortButton.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.inflate(R.menu.sort_menu_artist_only)
            popupMenu.menu.findItem(R.id.album_artist).isChecked =
                prefs.getBoolean("isDisplayingAlbumArtist", false)

            when (sortStatus) {
                0 -> {
                    popupMenu.menu.findItem(R.id.name).isChecked = true
                }

                1 -> {
                    popupMenu.menu.findItem(R.id.size).isChecked = true
                }
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.name -> {
                        if (!menuItem.isChecked) {
                            artistAdapter.sort(SupportComparator.createAlphanumericComparator { it.title })
                            menuItem.isChecked = true
                            sortStatus = 0
                        }
                    }

                    R.id.size -> {
                        if (!menuItem.isChecked) {
                            artistAdapter.sort(compareByDescending { it2 -> it2.songList.size })
                            menuItem.isChecked = true
                            sortStatus = 1
                        }
                    }

                    R.id.album_artist -> {
                        if (!prefs.getBoolean("isDisplayingAlbumArtist", false)) {
                            prefs.edit().putBoolean("isDisplayingAlbumArtist", true).apply()
                            var itemCount = 0
                            menuItem.isChecked = !menuItem.isChecked
                            viewModel.albumArtistItemList.value?.let { it1 ->
                                artistAdapter.updateList(
                                    it1
                                )
                                itemCount = it1.size
                            }
                            updateSongCounter(itemCount)
                            artistAdapter.setClickEventToAlbumArtist()
                        } else {
                            prefs.edit().putBoolean("isDisplayingAlbumArtist", false).apply()
                            var itemCount = 0
                            menuItem.isChecked = !menuItem.isChecked
                            viewModel.artistItemList.value?.let { it1 ->
                                artistAdapter.updateList(
                                    it1
                                )
                                itemCount = it1.size
                            }
                            updateSongCounter(itemCount)
                            artistAdapter.setClickEventToAlbumArtist(true)
                        }
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val sortButton: MaterialButton = view.findViewById(R.id.sort)
        val songCounter: TextView = view.findViewById(R.id.song_counter)
    }

    fun updateSongCounter(count: Int) {
        sortStatus = 0
        artistCount = count
        notifyItemChanged(0)
    }

    fun isCounterEmpty(): Boolean = artistCount == 0

    fun updateListToAlbumArtist() {
        prefs.edit().putBoolean("isDisplayingAlbumArtist", true).apply()
        var itemCount = 0
        viewModel.albumArtistItemList.value?.let { it1 ->
            artistAdapter.updateList(
                it1
            )
            itemCount = it1.size
        }
        updateSongCounter(itemCount)
        artistAdapter.setClickEventToAlbumArtist()
    }
}
