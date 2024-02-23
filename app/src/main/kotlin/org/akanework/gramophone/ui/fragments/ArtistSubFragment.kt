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
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.components.GridPaddingDecoration
import org.akanework.gramophone.ui.LibraryViewModel

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
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        gridPaddingDecoration = GridPaddingDecoration(requireContext())

        val rootView = inflater.inflate(R.layout.fragment_general_sub, container, false)
        val materialToolbar = rootView.findViewById<MaterialToolbar>(R.id.materialToolbar)
        val collapsingToolbarLayout = rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appBarLayout)

        val position = requireArguments().getInt("Position")
        val itemType = requireArguments().getInt("Item")
        recyclerView = rootView.findViewById(R.id.recyclerView)

        // https://github.com/material-components/material-components-android/issues/1310
        ViewCompat.setOnApplyWindowInsetsListener(collapsingToolbarLayout, null)

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
            val inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()))
            v.updatePadding(inset.left, inset.top, inset.right, 0)
            return@setOnApplyWindowInsetsListener insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()))
            v.updatePadding(inset.left, 0, inset.right, inset.bottom)
            return@setOnApplyWindowInsetsListener insets
        }

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
        recyclerView.layoutManager = GridLayoutManager(context,
            if (requireContext().resources.configuration.orientation
            == Configuration.ORIENTATION_PORTRAIT) 2 else 4).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    // BaseDecorAdapter always is full width
                    return if (position == 0 || position == albumAdapter.concatAdapter.itemCount)
                            (if (requireContext().resources.configuration.orientation
                                == Configuration.ORIENTATION_PORTRAIT) 2 else 4)
                    // One album takes 1 span, one song takes 2 spans
                    else if (position > 0 && position < albumAdapter.concatAdapter.itemCount) 1 else 2
                }
            }
        }
        recyclerView.adapter = ConcatAdapter(albumAdapter.concatAdapter, songAdapter.concatAdapter)
        recyclerView.addItemDecoration(gridPaddingDecoration)

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

        materialToolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        materialToolbar.title = item.title ?: requireContext().getString(R.string.unknown_artist)

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
