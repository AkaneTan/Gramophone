/*
 *     Copyright (C) 2023  Akane Foundation
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

package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.Sorter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@androidx.annotation.OptIn(UnstableApi::class)
class GeneralSubFragment : BaseFragment(true) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_general_sub, container, false)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appbarlayout)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val bundle = requireArguments()
        val title: String?
        val itemType = bundle.getInt("Item")
        val position = bundle.getInt("Position")
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        lateinit var itemList: List<MediaItem>
        var helper: Sorter.NaturalOrderHelper<MediaItem>? = null
        var isTrackDiscNumAvailable = false

        val processColor = ColorUtils.getColor(
            MaterialColors.getColor(
                topAppBar,
                android.R.attr.colorBackground
            ),
            ColorUtils.ColorType.COLOR_BACKGROUND,
            requireContext()
        )
        topAppBar.setBackgroundColor(processColor)
        appBarLayout.setBackgroundColor(processColor)

        when (itemType) {
            R.id.album -> {
                val item = libraryViewModel.albumItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_album)
                itemList = item.songList
                helper =
                    Sorter.NaturalOrderHelper { it.mediaMetadata.trackNumber!! + it.mediaMetadata.discNumber!! * 1000 }
                isTrackDiscNumAvailable = true
            }

            /*R.id.artist -> {
                val item = libraryViewModel.artistItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_artist)
                itemList = item.songList
            } TODO */

            R.id.genre -> {
                // Genres
                val item = libraryViewModel.genreItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_genre)
                itemList = item.songList
            }

            R.id.year -> {
                // Dates
                val item = libraryViewModel.dateItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_year)
                itemList = item.songList
            }

            /*R.id.album_artist -> {
                // Album artists
                val item = libraryViewModel.albumArtistItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_artist)
                itemList = item.songList
            } TODO */

            R.id.playlist -> {
                // Playlists
                val item = libraryViewModel.playlistList.value!![position]
                title = if (item is MediaStoreUtils.RecentlyAdded) {
                    requireContext().getString(R.string.recently_added)
                } else {
                    item.title ?: requireContext().getString(R.string.unknown_playlist)
                }
                itemList = item.songList
                helper = Sorter.NaturalOrderHelper { itemList.indexOf(it) }
            }

            else -> throw IllegalArgumentException()
        }

        val songAdapter =
            SongAdapter(requireActivity() as MainActivity, itemList, true, helper, true, isTrackDiscNumAvailable)
        recyclerView.adapter = songAdapter.concatAdapter

        FastScrollerBuilder(recyclerView).apply {
            useMd2Style()
            setPopupTextProvider(songAdapter)
            setTrackDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_transparent)!!)
            build()
        }

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        topAppBar.title = title

        return rootView
    }
}
