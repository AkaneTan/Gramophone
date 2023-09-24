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
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.GenreAdapter
import org.akanework.gramophone.ui.adapters.GenreDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel


/**
 * [GenreFragment] displays information about your song's genres.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class GenreFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val genreList = mutableListOf<MediaStoreUtils.Genre>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_genre, container, false)
        val genreRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        genreRecyclerView.layoutManager = LinearLayoutManager(activity)
        genreList.clear()
        genreList.addAll(libraryViewModel.genreItemList.value!!)
        val genreAdapter =
            GenreAdapter(
                genreList,
                requireContext(),
                requireActivity().supportFragmentManager,
                requireActivity() as MainActivity,
            )
        val genreDecorAdapter =
            GenreDecorAdapter(
                requireContext(),
                libraryViewModel.genreItemList.value!!.size,
                genreAdapter,
            )
        val concatAdapter = ConcatAdapter(genreDecorAdapter, genreAdapter)

        if (!libraryViewModel.genreItemList.hasActiveObservers()) {
            libraryViewModel.genreItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    genreDecorAdapter.updateSongCounter(mediaItems.size)
                    genreAdapter.updateList(mediaItems)
                }
            }
        }

        genreRecyclerView.adapter = concatAdapter

        FastScrollerBuilder(genreRecyclerView).apply {
            setPopupTextProvider(BaseAdapter.BasePopupTextProvider(genreAdapter))
            build()
        }

        return rootView
    }

}
