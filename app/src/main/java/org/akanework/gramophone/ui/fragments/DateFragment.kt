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
import org.akanework.gramophone.ui.adapters.DateAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [DateFragment] displays information about your song's dates.
 */
class DateFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val dateRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        dateRecyclerView.layoutManager = LinearLayoutManager(activity)
        val dateAdapter =
            DateAdapter(
                requireActivity() as MainActivity,
                libraryViewModel.dateItemList.value!!,
            )
        val dateDecorAdapter =
            BaseDecorAdapter(
                dateAdapter,
                R.plurals.items
            )
        val concatAdapter = ConcatAdapter(dateDecorAdapter, dateAdapter)

        if (!libraryViewModel.dateItemList.hasActiveObservers()) {
            libraryViewModel.dateItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    dateDecorAdapter.updateSongCounter(mediaItems.size)
                    dateAdapter.updateList(mediaItems)
                }
            }
        }

        dateRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(dateRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(dateAdapter))
            build()
        }

        return rootView
    }
}
