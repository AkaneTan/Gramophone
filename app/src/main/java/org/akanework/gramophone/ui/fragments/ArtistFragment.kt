package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.ArtistDecorAdapter
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [ArtistFragment] displays information about artists.
 */
class ArtistFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val artistRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        artistRecyclerView.layoutManager = LinearLayoutManager(activity)

        val artistAdapter =
            ArtistAdapter(
                requireActivity() as MainActivity,
                libraryViewModel.artistItemList.value!!,
            )
        val artistDecorAdapter =
            ArtistDecorAdapter(
                artistAdapter,
                prefs
            )
        val concatAdapter = ConcatAdapter(artistDecorAdapter, artistAdapter)

        if (prefs.getBoolean("isDisplayingAlbumArtist", false)) {
            artistDecorAdapter.updateListToAlbumArtist()
        }

        if (!libraryViewModel.artistItemList.hasActiveObservers()) {
            libraryViewModel.artistItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    artistDecorAdapter.updateSongCounter(mediaItems.size)
                    if (prefs.getBoolean("isDisplayingAlbumArtist", false)) {
                        artistDecorAdapter.updateListToAlbumArtist()
                    } else {
                        artistAdapter.updateList(mediaItems)
                    }
                }
            }
        }

        artistRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(artistRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(artistAdapter))
            build()
        }

        return rootView
    }
}
