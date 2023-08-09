package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.AlbumDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [AlbumFragment] displays information about your albums.
 */
class AlbumFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_album, container, false)
        val albumRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        albumRecyclerView.layoutManager = gridLayoutManager
        val albumList = mutableListOf<MediaStoreUtils.Album>()
        albumList.addAll(libraryViewModel.albumItemList.value!!)
        val albumAdapter = AlbumAdapter(albumList, requireActivity().supportFragmentManager)
        val albumDecorAdapter = AlbumDecorAdapter(requireContext(),
            libraryViewModel.albumItemList.value!!.size,
            albumAdapter)
        concatAdapter = ConcatAdapter(albumDecorAdapter, albumAdapter)

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
            useMd2Style()
            build()
        }
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gridLayoutManager = GridLayoutManager(activity, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (concatAdapter.getItemViewType(position) == AlbumDecorAdapter.VIEW_TYPE_ALBUM_DECOR) {
                    gridLayoutManager.spanCount // Return the total span count for albumDecorAdapter
                } else {
                    1 // For other view types, return 1 (normal span)
                }
            }
        }
    }
}