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
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@androidx.annotation.OptIn(UnstableApi::class)
class ArtistSubFragment : BaseFragment(true) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_general_sub, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val bundle = requireArguments()
        val position = bundle.getInt("Position")
        val itemType = bundle.getInt("Item")
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        lateinit var itemList: MutableList<MediaItem>
        val item = libraryViewModel.let { if (itemType == R.id.album_artist)
            it.albumArtistItemList else it.artistItemList }.value!![position]
        val title = item.title ?: requireContext().getString(R.string.unknown_artist)
        itemList = item.songList.toMutableList()

        val songAdapter = SongAdapter(requireActivity() as MainActivity, itemList, true, null, true)
        recyclerView.adapter = songAdapter.concatAdapter

        // TODO display albumList too

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
