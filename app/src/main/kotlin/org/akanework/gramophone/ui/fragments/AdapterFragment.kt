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

package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.ui.ItemHeightHelper
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.ui.LibraryViewModel
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.DateAdapter
import org.akanework.gramophone.ui.adapters.DetailedFolderAdapter
import org.akanework.gramophone.ui.adapters.FolderAdapter
import org.akanework.gramophone.ui.adapters.GenreAdapter
import org.akanework.gramophone.ui.adapters.PlaylistAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter

/**
 * AdapterFragment:
 *   This fragment is the container for any list that contains
 * recyclerview in the program.
 *
 * @author nift4
 */
class AdapterFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    private lateinit var adapter: BaseInterface<*>
    private lateinit var recyclerView: MyRecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        recyclerView = rootView.findViewById(R.id.recyclerview)
        recyclerView.enableEdgeToEdgePaddingListener()
        adapter = createAdapter(libraryViewModel)
        recyclerView.adapter = adapter.concatAdapter
        recyclerView.setAppBar((requireParentFragment() as ViewPagerFragment).appBarLayout)
        recyclerView.fastScroll(adapter, adapter.itemHeightHelper)
        return rootView
    }

    private fun createAdapter(v: LibraryViewModel): BaseInterface<*> {
        return when (arguments?.getInt("ID", -1)) {
            R.id.songs -> SongAdapter(this, v.mediaItemList, true, null, true)
            R.id.albums -> AlbumAdapter(this, v.albumItemList)
            R.id.artists -> ArtistAdapter(this, v.artistItemList, v.albumArtistItemList)
            R.id.genres -> GenreAdapter(this, v.genreItemList)
            R.id.dates -> DateAdapter(this, v.dateItemList)
            R.id.folders -> FolderAdapter(this, v.folderStructure)
            R.id.detailed_folders -> DetailedFolderAdapter(this, v.shallowFolderStructure)
            R.id.playlists -> PlaylistAdapter(this, v.playlistList)
            -1, null -> throw IllegalArgumentException("unset ID value")
            else -> throw IllegalArgumentException("invalid ID value")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // TODO is this really needed? if yes, move it to MyRecyclerView to make it global
        adapter.concatAdapter.adapters.forEach {
            it.onDetachedFromRecyclerView(recyclerView)
        }
    }

    abstract class BaseInterface<T : RecyclerView.ViewHolder>
        : MyRecyclerView.Adapter<T>(), PopupTextProvider {
        abstract val concatAdapter: ConcatAdapter
        abstract val itemHeightHelper: ItemHeightHelper?
    }
}
