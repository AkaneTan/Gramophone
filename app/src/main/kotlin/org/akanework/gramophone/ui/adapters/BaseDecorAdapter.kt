/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.media3.common.C
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.FileOpUtils
import kotlin.random.Random

open class BaseDecorAdapter<T : BaseAdapter<*>>(
    protected val adapter: T,
    private val pluralStr: Int,
    private val isSubFragment: Boolean = false
) : RecyclerView.Adapter<BaseDecorAdapter<T>.ViewHolder>() {

    protected val context: Context = adapter.context

    private var prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.general_decor, parent, false)
        return ViewHolder(view)
    }

    final override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val count = adapter.itemCount
        holder.playAll.visibility =
            if (adapter is SongAdapter && adapter.sortType != Sorter.Type.None) View.VISIBLE else View.GONE
        holder.shuffleAll.visibility =
            if (adapter is SongAdapter && adapter.sortType != Sorter.Type.None) View.VISIBLE else View.GONE
        holder.counter.text = context.resources.getQuantityString(pluralStr, count, count)
        holder.sortButton.visibility =
            if (adapter.sortType != Sorter.Type.None || adapter.ownsView) View.VISIBLE else View.GONE
        holder.sortButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.sort_menu)
            val buttonMap = mapOf(
                Pair(R.id.natural, Sorter.Type.NaturalOrder),
                Pair(R.id.name, if (adapter.sortTypes.contains(Sorter.Type.NativeOrder))
                    Sorter.Type.NativeOrder else Sorter.Type.ByTitleAscending),
                Pair(R.id.artist, Sorter.Type.ByArtistAscending),
                Pair(R.id.album, Sorter.Type.ByAlbumTitleAscending),
                Pair(R.id.size, Sorter.Type.BySizeDescending),
                Pair(R.id.add_date, Sorter.Type.ByAddDateDescending),
                Pair(R.id.mod_date, Sorter.Type.ByModifiedDateDescending)
            )
            val layoutMap = mapOf(
                Pair(R.id.list, BaseAdapter.LayoutType.LIST),
                Pair(R.id.compact_list, BaseAdapter.LayoutType.COMPACT_LIST),
                Pair(R.id.grid, BaseAdapter.LayoutType.GRID)
            )
            buttonMap.forEach {
                popupMenu.menu.findItem(it.key).isVisible = adapter.sortTypes.contains(it.value)
            }
            layoutMap.forEach {
                popupMenu.menu.findItem(it.key).isVisible = adapter.ownsView
            }
            popupMenu.menu.findItem(R.id.display).isVisible = adapter.ownsView
            if (adapter.sortType != Sorter.Type.None) {
                when (adapter.sortType) {
                    in buttonMap.values -> {
                        popupMenu.menu.findItem(
                            buttonMap.entries
                                .first { it.value == adapter.sortType }.key
                        ).isChecked = true
                    }

                    else -> throw IllegalStateException("Invalid sortType ${adapter.sortType.name}")
                }
            }
            if (adapter.ownsView) {
                when (adapter.layoutType) {
                    in layoutMap.values -> {
                        popupMenu.menu.findItem(
                            layoutMap.entries
                                .first { it.value == adapter.layoutType }.key
                        ).isChecked = true
                    }

                    else -> throw IllegalStateException("Invalid layoutType ${adapter.layoutType?.name}")
                }
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    in buttonMap.keys -> {
                        if (!menuItem.isChecked) {
                            adapter.sort(buttonMap[menuItem.itemId]!!)
                            menuItem.isChecked = true
                            if (!isSubFragment) {
                                prefs.edit()
                                    .putString(
                                        "S" + FileOpUtils.getAdapterType(adapter).toString(),
                                        buttonMap[menuItem.itemId].toString()
                                    )
                                    .apply()
                            }
                        }
                        true
                    }

                    in layoutMap.keys -> {
                        if (!menuItem.isChecked) {
                            adapter.layoutType = layoutMap[menuItem.itemId]!!
                            menuItem.isChecked = true
                            if (!isSubFragment) {
                                prefs.edit()
                                    .putString(
                                        "L" + FileOpUtils.getAdapterType(adapter).toString(),
                                        layoutMap[menuItem.itemId].toString()
                                    )
                                    .apply()
                            }
                        }
                        true
                    }

                    else -> onExtraMenuButtonPressed(menuItem)
                }
            }
            onSortButtonPressed(popupMenu)
            popupMenu.show()
        }
        holder.playAll.setOnClickListener {
            if (adapter is SongAdapter) {
                val mediaController = adapter.getActivity().getPlayer()
                val songList = adapter.getSongList()
                mediaController.setMediaItems(songList, 0, C.TIME_UNSET)
                mediaController.prepare()
                mediaController.play()
            }
        }
        holder.shuffleAll.setOnClickListener {
            if (adapter is SongAdapter) {
                val mediaController = adapter.getActivity().getPlayer()
                val songList = adapter.getSongList()
                mediaController.setMediaItems(songList, 0, C.TIME_UNSET)
                mediaController.shuffleModeEnabled = true
                mediaController.seekToDefaultPosition(Random.nextInt(0, adapter.getSongList().size))
                mediaController.prepare()
                mediaController.play()
            }
        }
    }

    protected open fun onSortButtonPressed(popupMenu: PopupMenu) {}
    protected open fun onExtraMenuButtonPressed(menuItem: MenuItem): Boolean = false

    override fun getItemCount(): Int = 1

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val sortButton: MaterialButton = view.findViewById(R.id.sort)
        val playAll: MaterialButton = view.findViewById(R.id.play_all)
        val shuffleAll: MaterialButton = view.findViewById(R.id.shuffle_all)
        val counter: TextView = view.findViewById(R.id.song_counter)
    }

    fun updateSongCounter() {
        notifyItemChanged(0)
    }
}