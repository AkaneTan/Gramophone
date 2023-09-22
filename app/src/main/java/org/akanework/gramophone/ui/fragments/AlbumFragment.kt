package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.AlbumDecorAdapter
import org.akanework.gramophone.ui.components.CustomGridLayoutManager
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [AlbumFragment] displays information about your albums.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class AlbumFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val albumList = mutableListOf<MediaStoreUtils.Album>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_album, container, false)
        val albumRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        albumList.clear()
        albumList.addAll(libraryViewModel.albumItemList.value!!)
        val albumAdapter =
            AlbumAdapter(
                albumList,
                requireActivity().supportFragmentManager,
                requireActivity() as MainActivity,
            )
        val albumDecorAdapter =
            AlbumDecorAdapter(
                requireContext(),
                libraryViewModel.albumItemList.value!!.size,
                albumAdapter,
            )
        val concatAdapter = ConcatAdapter(albumDecorAdapter, albumAdapter)

        val gridLayoutManager = CustomGridLayoutManager(requireContext(), 2)

        albumRecyclerView.layoutManager = gridLayoutManager

        if (!libraryViewModel.albumItemList.hasActiveObservers()) {
            libraryViewModel.albumItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    if (mediaItems.size != albumList.size || albumDecorAdapter.isCounterEmpty()) {
                        albumDecorAdapter.updateSongCounter(mediaItems.size)
                    }
                    albumAdapter.updateList(mediaItems)
                }
            }
        }

        albumRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(albumRecyclerView).apply {
            setPopupTextProvider(AlbumPopupTextProvider())
            useMd2Style()
            build()
        }

        return rootView
    }

    inner class AlbumPopupTextProvider : PopupTextProvider {
        override fun getPopupText(position: Int): CharSequence {
            if (position != 0) {
                return albumList[position - 1].title.first().toString()
            }
            return "-"
        }
    }
}
