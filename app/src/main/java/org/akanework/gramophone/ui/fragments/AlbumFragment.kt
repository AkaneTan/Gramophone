package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.BaseDecorAdapter
import org.akanework.gramophone.ui.components.CustomGridLayoutManager
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [AlbumFragment] displays information about your albums.
 */
class AlbumFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val albumRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        val albumAdapter =
            AlbumAdapter(
                requireActivity() as MainActivity,
                libraryViewModel.albumItemList,
            )
        val gridLayoutManager = CustomGridLayoutManager(requireContext(), 2)

        albumRecyclerView.layoutManager = gridLayoutManager
        albumRecyclerView.adapter = albumAdapter.concatAdapter

        FastScrollerBuilder(albumRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(albumAdapter))
            build()
        }

        return rootView
    }
}
