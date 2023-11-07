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
import com.google.android.material.appbar.MaterialToolbar
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

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
        val position = requireArguments().getInt("Position")
        val itemType = requireArguments().getInt("Item")
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        val item = libraryViewModel.let {
            if (itemType == R.id.album_artist)
                it.albumArtistItemList else it.artistItemList
        }.value!![position]
        albumAdapter =
            AlbumAdapter(requireActivity() as MainActivity, item.albumList.toMutableList())
        songAdapter = SongAdapter(
            requireActivity() as MainActivity,
            item.songList, true, null, false
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
            setTrackDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_transparent)!!)
            useMd2Style()
            build()
        }

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        topAppBar.title = item.title ?: requireContext().getString(R.string.unknown_artist)

        return rootView
    }

    override fun getPopupText(position: Int): CharSequence {
        return if (position < albumAdapter.itemCount) {
            albumAdapter.getPopupText(position)
        } else {
            songAdapter.getPopupText(position - albumAdapter.itemCount)
        }
    }
}
