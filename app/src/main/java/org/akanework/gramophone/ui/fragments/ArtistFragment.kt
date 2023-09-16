package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.ArtistDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [ArtistFragment] displays information about artists.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class ArtistFragment : BaseFragment(false) {

    companion object {
        const val TAG = "ArtistFragment"
    }

    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val artistList = mutableListOf<MediaStoreUtils.Artist>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_artist, container, false)
        val artistRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        artistRecyclerView.layoutManager = LinearLayoutManager(activity)
        artistList.clear()
        artistList.addAll(libraryViewModel.artistItemList.value!!)
        val albumArtistList = mutableListOf<MediaStoreUtils.Artist>()
        albumArtistList.addAll(libraryViewModel.albumArtistItemList.value!!)

        val artistAdapter =
            ArtistAdapter(
                artistList,
                albumArtistList,
                requireContext(),
                requireActivity().supportFragmentManager,
                requireActivity() as MainActivity,
            )
        val artistDecorAdapter =
            ArtistDecorAdapter(
                requireContext(),
                libraryViewModel.artistItemList.value!!.size,
                artistAdapter,
                requireActivity() as MainActivity,
                prefs
            )
        val concatAdapter = ConcatAdapter(artistDecorAdapter, artistAdapter)

        if (prefs.getBoolean("isDisplayingAlbumArtist", false)) {
            artistDecorAdapter.updateListToAlbumArtist()
        }

        if (!libraryViewModel.artistItemList.hasActiveObservers()) {
            libraryViewModel.artistItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    if (mediaItems.size != artistList.size || artistDecorAdapter.isCounterEmpty()) {
                        artistDecorAdapter.updateSongCounter(mediaItems.size)
                    }
                    artistAdapter.updateList(mediaItems)
                    if (prefs.getBoolean("isDisplayingAlbumArtist", false)) {
                        artistDecorAdapter.updateListToAlbumArtist()
                    }
                }
            }
        }

        artistRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(artistRecyclerView).apply {
            setPopupTextProvider(ArtistPopupTextProvider())
            build()
        }

        return rootView
    }

    inner class ArtistPopupTextProvider : PopupTextProvider {
        override fun getPopupText(position: Int): CharSequence {
            if (position != 0) {
                return artistList[position - 1].title.first().toString()
            }
            return "-"
        }
    }
}
