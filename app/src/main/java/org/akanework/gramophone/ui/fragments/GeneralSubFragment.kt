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
import com.google.android.material.appbar.MaterialToolbar
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.SongDecorAdapter
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
        val title = bundle.getString("Title")
        val item = bundle.getInt("Item")
        val position = bundle.getInt("Position")
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        lateinit var itemList: MutableList<MediaItem>
        var canSort = true

        when (item) {
            1 -> {
                // Albums
                itemList =
                    libraryViewModel
                        .albumItemList
                        .value!![position]
                        .songList
                        .toMutableList()
            }

            2 -> {
                // Artists
                itemList =
                    libraryViewModel
                        .artistItemList
                        .value!![position]
                        .songList
                        .toMutableList()
            }

            3 -> {
                // Genres
                itemList =
                    libraryViewModel
                        .genreItemList
                        .value!![position]
                        .songList
                        .toMutableList()
            }

            4 -> {
                // Dates
                itemList =
                    libraryViewModel
                        .dateItemList
                        .value!![position]
                        .songList
                        .toMutableList()
            }

            5 -> {
                // Album artists
                itemList =
                    libraryViewModel
                        .albumArtistItemList
                        .value!![position]
                        .songList
                        .toMutableList()
            }

            6 -> {
                // Playlists
                canSort = false // Playlists have some order already
                itemList =
                    libraryViewModel
                        .playlistList
                        .value!![position]
                        .songList
                        .toMutableList()
            }
        }

        val songAdapter = SongAdapter(itemList, requireActivity() as MainActivity, canSort)
        val songDecorAdapter =
            SongDecorAdapter(
                requireContext(),
                itemList.size,
                songAdapter,
                canSort,
            )
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = concatAdapter

        FastScrollerBuilder(recyclerView).build()

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        topAppBar.title = title

        return rootView
    }
}
