package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
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
class SongFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val songList = mutableListOf<MediaItem>()
    private lateinit var songAdapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_song, container, false)
        val songRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        songRecyclerView.layoutManager = LinearLayoutManager(activity)
        songList.clear()
        songList.addAll(libraryViewModel.mediaItemList.value!!)
        songAdapter = SongAdapter(songList, requireActivity() as MainActivity, true)
        val songDecorAdapter =
            SongDecorAdapter(
                requireContext(),
                libraryViewModel.mediaItemList.value!!.size,
                songAdapter,
                true,
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

        FastScrollerBuilder(songRecyclerView).apply {
            setPopupTextProvider(SongPopupTextProvider())
            build()
        }

        return rootView
    }

    inner class SongPopupTextProvider : PopupTextProvider {
        override fun getPopupText(position: Int): CharSequence {
            if (position != 0) {
                return songAdapter.getFrozenList()[position - 1].mediaMetadata.title?.first().toString()
            }
            return "-"
        }
    }
}
