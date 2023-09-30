package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [ArtistFragment] displays information about artists.
 */
class ArtistFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val artistRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        artistRecyclerView.layoutManager = LinearLayoutManager(activity)

        val artistAdapter =
            ArtistAdapter(
                requireActivity() as MainActivity,
                libraryViewModel.artistItemList,
                libraryViewModel.albumArtistItemList,
            )

        artistRecyclerView.adapter = artistAdapter.concatAdapter

        FastScrollerBuilder(artistRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(artistAdapter))
            build()
        }

        return rootView
    }
}
