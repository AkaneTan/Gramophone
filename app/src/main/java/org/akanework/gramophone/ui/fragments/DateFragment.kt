package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.DateAdapter
import org.akanework.gramophone.ui.adapters.DateDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * [DateFragment] displays information about your song's dates.
 */
class DateFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_date, container, false)
        val dateRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        dateRecyclerView.layoutManager = LinearLayoutManager(activity)
        val dateList = mutableListOf<MediaStoreUtils.Date>()
        dateList.addAll(libraryViewModel.dateItemList.value!!)
        val dateAdapter = DateAdapter(dateList, requireContext())
        val dateDecorAdapter = DateDecorAdapter(requireContext(),
            libraryViewModel.dateItemList.value!!.size,
            dateAdapter)
        val concatAdapter = ConcatAdapter(dateDecorAdapter, dateAdapter)

        if (!libraryViewModel.dateItemList.hasActiveObservers()) {
            libraryViewModel.dateItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    if (mediaItems.size != dateList.size || dateDecorAdapter.isCounterEmpty()) {
                        dateDecorAdapter.updateSongCounter(mediaItems.size)
                    }
                    dateAdapter.updateList(mediaItems)
                }
            }
        }

        dateRecyclerView.adapter = concatAdapter
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}