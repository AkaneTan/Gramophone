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

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.ui.DefaultItemHeightHelper
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.ui.LibraryViewModel
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.components.GridPaddingDecoration
import kotlin.properties.Delegates

/**
 * ArtistSubFragment:
 *   Separated from GeneralSubFragment and will be
 * merged into it in future development.
 *
 * @author nift4
 * @see BaseFragment
 * @see GeneralSubFragment
 */
@androidx.annotation.OptIn(UnstableApi::class)
class ArtistSubFragment : BaseFragment(true), PopupTextProvider {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var songAdapter: SongAdapter
    private lateinit var gridPaddingDecoration: GridPaddingDecoration
    private lateinit var recyclerView: MyRecyclerView
    private var spans by Delegates.notNull<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        gridPaddingDecoration = GridPaddingDecoration(requireContext())

        val rootView = inflater.inflate(R.layout.fragment_general_sub, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appbarlayout)
        appBarLayout.enableEdgeToEdgePaddingListener()

        val position = requireArguments().getInt("Position")
        val itemType = requireArguments().getInt("Item")
        recyclerView = rootView.findViewById(R.id.recyclerview)

        val item = libraryViewModel.let {
            if (itemType == R.id.album_artist)
                it.albumArtistItemList else it.artistItemList
        }.value!![position]
        albumAdapter =
            AlbumAdapter(requireActivity() as MainActivity, item.albumList.toMutableList(), true)
        albumAdapter.decorAdapter.jumpDownPos = albumAdapter.concatAdapter.itemCount
        songAdapter = SongAdapter(
            requireActivity() as MainActivity,
            item.songList, true, null, false,
            isSubFragment = true
        )
        songAdapter.decorAdapter.jumpUpPos = 0
        spans = if (requireContext().resources.configuration.orientation
            == Configuration.ORIENTATION_PORTRAIT) 2 else 4
        recyclerView.layoutManager = GridLayoutManager(context, spans).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    // BaseDecorAdapter always is full width
                    return if (position == 0 || position == albumAdapter.concatAdapter.itemCount) spans
                    // One album takes 1 span, one song takes 2 spans
                    else if (position > 0 && position < albumAdapter.concatAdapter.itemCount) 1 else 2
                }
            }
        }
        val albumsIh = DefaultItemHeightHelper.concatItemHeightHelper(albumAdapter.decorAdapter, {1}) {
            val albumCount = ((albumAdapter.itemCount / spans.toFloat()) + 0.5f).toInt()
            val nto = (it / spans.toFloat()).toInt().coerceAtMost(albumCount)
            albumAdapter.getItemHeightFromZeroTo(nto)
        }
        val songsIh = DefaultItemHeightHelper.concatItemHeightHelper(songAdapter.decorAdapter, {1}) {
            val songCount = ((songAdapter.itemCount / (spans / 2f)) + 0.5f).toInt()
            val nto = (it / (spans / 2f)).toInt().coerceAtMost(songCount)
            songAdapter.getItemHeightFromZeroTo(nto)
        }
        val ih = DefaultItemHeightHelper.concatItemHeightHelper(albumsIh, { albumAdapter.itemCount + 1 }, songsIh)
        recyclerView.enableEdgeToEdgePaddingListener()
        recyclerView.adapter = ConcatAdapter(albumAdapter.concatAdapter, songAdapter.concatAdapter)
        recyclerView.addItemDecoration(gridPaddingDecoration)
        recyclerView.setAppBar(appBarLayout)
        recyclerView.fastScroll(this, ih)

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        topAppBar.title = item.title ?: requireContext().getString(R.string.unknown_artist)

        return rootView
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        return if (position < albumAdapter.concatAdapter.itemCount) {
            albumAdapter.getPopupText(view, position)
        } else {
            songAdapter.getPopupText(view, position - albumAdapter.concatAdapter.itemCount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.removeItemDecoration(gridPaddingDecoration)
    }
}
