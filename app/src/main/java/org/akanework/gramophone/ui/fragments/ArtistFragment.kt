package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.ArtistDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [ArtistFragment] displays information about artists.
 */
class ArtistFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_artist, container, false)
        val artistRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        artistRecyclerView.layoutManager = LinearLayoutManager(activity)
        val artistList = mutableListOf<MediaStoreUtils.Artist>()
        artistList.addAll(libraryViewModel.artistItemList.value!!)
        val artistAdapter = ArtistAdapter(artistList, requireContext())
        val artistDecorAdapter = ArtistDecorAdapter(requireContext(),
            libraryViewModel.artistItemList.value!!.size,
            artistAdapter)
        val concatAdapter = ConcatAdapter(artistDecorAdapter, artistAdapter)

        if (!libraryViewModel.artistItemList.hasActiveObservers()) {
            libraryViewModel.artistItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    if (mediaItems.size != artistList.size || artistDecorAdapter.isCounterEmpty()) {
                        artistDecorAdapter.updateSongCounter(mediaItems.size)
                    }
                    artistAdapter.updateList(mediaItems)
                }
            }
        }

        artistRecyclerView.adapter = concatAdapter
        return rootView
    }
}