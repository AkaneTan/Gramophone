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
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_general_sub, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appbarlayout)

        val position = requireArguments().getInt("Position")
        val itemType = requireArguments().getInt("Item")
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        val item = libraryViewModel.let {
            if (itemType == R.id.album_artist)
                it.albumArtistItemList else it.artistItemList
        }.value!![position]
        albumAdapter =
            AlbumAdapter(requireActivity() as MainActivity, item.albumList.toMutableList(), true)
        songAdapter = SongAdapter(
            requireActivity() as MainActivity,
            item.songList, true, null, false,
            isSubFragment = true
        )
        recyclerView.layoutManager = GridLayoutManager(context, 2).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position > 0 && position <= albumAdapter.itemCount) 1 else 2
                }
            }
        }
        recyclerView.adapter = ConcatAdapter(albumAdapter.concatAdapter, songAdapter.concatAdapter)

        FastScrollerBuilder(recyclerView).apply {
            setPopupTextProvider(this@ArtistSubFragment)
            setTrackDrawable(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_transparent
                )!!
            )
            useMd2Style()
            build()
        }

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        topAppBar.title = item.title ?: requireContext().getString(R.string.unknown_artist)

        return rootView
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        return if (position < albumAdapter.itemCount) {
            albumAdapter.getPopupText(view, position)
        } else {
            songAdapter.getPopupText(view, position - albumAdapter.itemCount)
        }
    }
}
