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
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.DateAdapter
import org.akanework.gramophone.ui.adapters.DateDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [DateFragment] displays information about your song's dates.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class DateFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_date, container, false)
        val dateRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        dateRecyclerView.layoutManager = LinearLayoutManager(activity)
        val dateList = mutableListOf<MediaStoreUtils.Date>()
        dateList.addAll(libraryViewModel.dateItemList.value!!)
        val dateAdapter =
            DateAdapter(
                dateList,
                requireContext(),
                requireActivity().supportFragmentManager,
                requireActivity() as MainActivity,
            )
        val dateDecorAdapter =
            DateDecorAdapter(
                requireContext(),
                libraryViewModel.dateItemList.value!!.size,
                dateAdapter,
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
