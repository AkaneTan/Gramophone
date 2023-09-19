package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.PlaylistAdapter
import org.akanework.gramophone.ui.adapters.PlaylistDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [PlaylistFragment] displays playlist information.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaylistFragment : BaseFragment(false) {

    companion object {
        const val TAG = "PlaylistFragment"
    }

    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val playlistList = mutableListOf<MediaStoreUtils.Playlist>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_playlist, container, false)
        val playlistRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        playlistRecyclerView.layoutManager = LinearLayoutManager(activity)
        playlistList.clear()
        playlistList.addAll(libraryViewModel.playlistList.value!!)

        val playlistAdapter =
            PlaylistAdapter(
                playlistList,
                requireContext(),
                requireActivity().supportFragmentManager,
                requireActivity() as MainActivity,
            )

        val playlistDecorAdapter =
            PlaylistDecorAdapter(
                requireContext(),
                libraryViewModel.playlistList.value!!.size,
                playlistAdapter
            )

        val concatAdapter = ConcatAdapter(playlistDecorAdapter, playlistAdapter)

        if (!libraryViewModel.playlistList.hasActiveObservers()) {
            libraryViewModel.playlistList.observe(viewLifecycleOwner) { mediaItems ->
                playlistAdapter.updateList(mediaItems)
            }
        }

        playlistRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(playlistRecyclerView).apply {
            setPopupTextProvider(PlaylistPopupTextProvider())
            build()
        }

        return rootView
    }

    inner class PlaylistPopupTextProvider : PopupTextProvider {
        override fun getPopupText(position: Int): CharSequence {
            if (position != 0) {
                return playlistList[position - 1].title.first().toString()
            }
            return "-"
        }
    }
}
