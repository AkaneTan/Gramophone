package org.akanework.serendipity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.serendipity.R
import org.akanework.serendipity.logic.utils.MediaStoreUtils
import org.akanework.serendipity.ui.adapters.AlbumAdapter
import org.akanework.serendipity.ui.adapters.AlbumDecorAdapter
import org.akanework.serendipity.ui.viewmodels.LibraryViewModel

/**
 * [AlbumFragment] displays information about your albums.
 */
class AlbumFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_album, container, false)
        val albumRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        val gridLayoutManager = GridLayoutManager(activity, 2)
        albumRecyclerView.layoutManager = gridLayoutManager
        val albumList = mutableListOf<MediaStoreUtils.Album>()
        albumList.addAll(libraryViewModel.albumItemList.value!!)
        val albumAdapter = AlbumAdapter(albumList)
        val albumDecorAdapter = AlbumDecorAdapter(requireContext(),
            libraryViewModel.albumItemList.value!!.size,
            albumAdapter)
        val concatAdapter = ConcatAdapter(albumDecorAdapter, albumAdapter)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (concatAdapter.getItemViewType(position) == AlbumDecorAdapter.VIEW_TYPE_ALBUM_DECOR) {
                    gridLayoutManager.spanCount // Return the total span count for albumDecorAdapter
                } else {
                    1 // For other view types, return 1 (normal span)
                }
            }
        }

        libraryViewModel.albumItemList.observe(viewLifecycleOwner) { mediaItems ->
            if (mediaItems.isNotEmpty()) {
                albumAdapter.updateList(mediaItems)
                albumDecorAdapter.updateSongCounter(mediaItems.size)
            }
        }

        albumRecyclerView.adapter = concatAdapter
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}