package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.BaseInterface
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

class AdapterFragment(private val adapterCreator:
                          (MainActivity, LibraryViewModel) -> BaseInterface<*>)
    : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = adapterCreator(requireActivity() as MainActivity, libraryViewModel)
        recyclerView.adapter = adapter.concatAdapter
        FastScrollerBuilder(recyclerView).apply {
            setPopupTextProvider(adapter)
            build()
        }
        return rootView
    }
}
