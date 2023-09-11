package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialSharedAxis
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.SongDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [SongFragment] is the default fragment that will show up
 * when you open the application. It displays information
 * about songs.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class SongFragment : BaseFragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_song, container, false)
        val songRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        songRecyclerView.layoutManager = LinearLayoutManager(activity)
        val songList = mutableListOf<MediaItem>()
        songList.addAll(libraryViewModel.mediaItemList.value!!)
        val songAdapter = SongAdapter(songList, requireActivity() as MainActivity)
        val songDecorAdapter =
            SongDecorAdapter(
                requireContext(),
                libraryViewModel.mediaItemList.value!!.size,
                songAdapter,
            )
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)

        if (!libraryViewModel.mediaItemList.hasActiveObservers()) {
            libraryViewModel.mediaItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    if (mediaItems.size != songList.size || songDecorAdapter.isCounterEmpty()) {
                        songDecorAdapter.updateSongCounter(mediaItems.size)
                    }
                    songAdapter.updateList(mediaItems)
                }
            }
        }

        songRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(songRecyclerView).build()

        return rootView
    }
}
