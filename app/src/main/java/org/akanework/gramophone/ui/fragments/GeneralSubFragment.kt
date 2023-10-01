package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.Sorter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@androidx.annotation.OptIn(UnstableApi::class)
class GeneralSubFragment : BaseFragment(true) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_general_sub, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val bundle = requireArguments()
        var title: String? = null
        val itemType = bundle.getInt("Item")
        val position = bundle.getInt("Position")
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        lateinit var itemList: MutableList<MediaItem>
        var helper: Sorter.NaturalOrderHelper<MediaItem>? = null

        when (itemType) {
            R.id.album -> {
                val item = libraryViewModel.albumItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_album)
                itemList = item.songList.toMutableList()
                helper = Sorter.NaturalOrderHelper { it.mediaMetadata.trackNumber!! }
            }

            R.id.artist -> {
                val item = libraryViewModel.artistItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_artist)
                itemList = item.songList.toMutableList()
            }

            R.id.genre -> {
                // Genres
                val item = libraryViewModel.genreItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_genre)
                itemList = item.songList.toMutableList()
            }

            R.id.year -> {
                // Dates
                val item = libraryViewModel.dateItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_year)
                itemList = item.songList.toMutableList()
            }

            R.id.album_artist -> {
                // Album artists
                val item = libraryViewModel.albumArtistItemList.value!![position]
                title = item.title ?: requireContext().getString(R.string.unknown_artist)
                itemList = item.songList.toMutableList()
            }

            R.id.playlist -> {
                // Playlists
                val item = libraryViewModel.playlistList.value!![position]
                title = if (item is MediaStoreUtils.RecentlyAdded) {
                    requireContext().getString(R.string.recently_added)
                } else {
                    item.title ?: requireContext().getString(R.string.unknown_playlist)
                }
                itemList = item.songList.toMutableList()
                helper = Sorter.NaturalOrderHelper { itemList.indexOf(it) }
            }
        }

        val songAdapter = SongAdapter(requireActivity() as MainActivity, itemList, true, helper, true)
        recyclerView.adapter = songAdapter.concatAdapter

        FastScrollerBuilder(recyclerView).apply {
            setPopupTextProvider(songAdapter)
            build()
        }

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        topAppBar.title = title

        return rootView
    }
}
