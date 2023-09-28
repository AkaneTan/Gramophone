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
import org.akanework.gramophone.ui.adapters.PlaylistAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [PlaylistFragment] displays playlist information.
 */
class PlaylistFragment : BaseFragment(false) {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val playlistRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        playlistRecyclerView.layoutManager = LinearLayoutManager(activity)

        val playlistAdapter =
            PlaylistAdapter(
                requireActivity() as MainActivity,
                libraryViewModel.playlistList.value!!,
            )

        if (!libraryViewModel.playlistList.hasActiveObservers()) {
            libraryViewModel.playlistList.observe(viewLifecycleOwner) { mediaItems ->
                playlistAdapter.updateList(mediaItems)
            }
        }

        playlistRecyclerView.adapter = playlistAdapter.concatAdapter

        FastScrollerBuilder(playlistRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(playlistAdapter))
            build()
        }

        return rootView
    }
}
