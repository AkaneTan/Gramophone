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
import org.akanework.gramophone.ui.adapters.GenreAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [GenreFragment] displays information about your song's genres.
 */
class GenreFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val genreRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        genreRecyclerView.layoutManager = LinearLayoutManager(activity)
        val genreAdapter =
            GenreAdapter(
                requireActivity() as MainActivity,
                libraryViewModel.genreItemList.value!!
            )

        if (!libraryViewModel.genreItemList.hasActiveObservers()) {
            libraryViewModel.genreItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    genreAdapter.updateList(mediaItems)
                }
            }
        }

        genreRecyclerView.adapter = genreAdapter.concatAdapter

        FastScrollerBuilder(genreRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(genreAdapter))
            build()
        }

        return rootView
    }

}
