package org.akanework.serendipity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.serendipity.R
import org.akanework.serendipity.ui.adapters.SongAdapter
import org.akanework.serendipity.ui.adapters.SongDecorAdapter
import org.akanework.serendipity.ui.viewmodels.LibraryViewModel

/**
 * [SongFragment] is the default fragment that will show up
 * when you open the application. It displays information
 * about songs.
 */
class SongFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_song, container, false)
        val songRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        songRecyclerView.layoutManager = LinearLayoutManager(activity)
        val songList = mutableListOf<MediaItem>()
        songList.addAll(libraryViewModel.mediaItemList.value!!)
        val songAdapter = SongAdapter(songList)
        val songDecorAdapter = SongDecorAdapter(requireContext(),
            libraryViewModel.mediaItemList.value!!.size,
            songAdapter)
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)

        libraryViewModel.mediaItemList.observe(viewLifecycleOwner) { mediaItems ->
            if (mediaItems.isNotEmpty()) {
                songAdapter.updateList(mediaItems)
                songDecorAdapter.updateSongCounter(mediaItems.size)
            }
        }

        songRecyclerView.adapter = concatAdapter
        return rootView
    }
}