package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.BaseDecorAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [SongFragment] is the default fragment that will show up
 * when you open the application. It displays information
 * about songs.
 */
class SongFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val songRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        songRecyclerView.layoutManager = LinearLayoutManager(activity)
        val songAdapter = SongAdapter(
            requireActivity() as MainActivity,
            libraryViewModel.mediaItemList.value!!, true)
        val songDecorAdapter =
            BaseDecorAdapter(
                songAdapter,
                R.plurals.songs
            )
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)

        if (!libraryViewModel.mediaItemList.hasActiveObservers()) {
            libraryViewModel.mediaItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    songDecorAdapter.updateSongCounter(mediaItems.size)
                    songAdapter.updateList(mediaItems)
                }
            }
        }

        songRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(songRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(songAdapter))
            build()
        }

        return rootView
    }
}
