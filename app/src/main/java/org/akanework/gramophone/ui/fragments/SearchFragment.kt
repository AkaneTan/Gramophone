package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.closeKeyboard
import org.akanework.gramophone.logic.utils.dp
import org.akanework.gramophone.logic.utils.showSoftKeyboard
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.SongDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel


@androidx.annotation.OptIn(UnstableApi::class)
class SearchFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val filteredList: MutableList<MediaItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_search, container, false)
        val editText = rootView.findViewById<EditText>(R.id.edit_text)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val songAdapter = SongAdapter(mutableListOf(), requireActivity() as MainActivity, false)
        val songDecorAdapter =
            SongDecorAdapter(
                requireContext(),
                0,
                songAdapter,
                false,
            )
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)
        val returnButton = rootView.findViewById<MaterialButton>(R.id.return_button)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = concatAdapter

        FastScrollerBuilder(recyclerView).apply {
            setPadding(0, 0, 0, (66).dp)
            setPopupTextProvider(SongPopupTextProvider())
            build()
        }

        editText.addTextChangedListener { text ->
            if (text.isNullOrBlank()) {
                songAdapter.updateList(mutableListOf())
                songDecorAdapter.updateSongCounter(0)
            } else {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    filteredList.clear()
                    libraryViewModel.mediaItemList.value?.filter {
                        val isMatchingTitle = it.mediaMetadata.title!!.contains(text, true)
                        val isMatchingAlbum =
                            it.mediaMetadata.albumTitle!!.contains(text, true)
                        val isMatchingArtist =
                            it.mediaMetadata.artist!!.contains(text, true)
                        isMatchingTitle || isMatchingAlbum || isMatchingArtist
                    }?.let {
                        filteredList.addAll(
                            it
                        )
                    }
                    withContext(Dispatchers.Main) {
                        if (filteredList.isNotEmpty()) {
                            songDecorAdapter.updateSongCounter(filteredList.size)
                        } else {
                            songDecorAdapter.updateSongCounter(0)
                        }
                        songAdapter.updateList(filteredList.toMutableList())
                    }
                }
            }
        }

        returnButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return rootView
    }

    override fun onPause() {
        super.onPause()
        requireActivity().closeKeyboard()
    }

    override fun onResume() {
        super.onResume()
        val editText = requireView().findViewById<EditText>(R.id.edit_text)
        requireView().post {
            editText.showSoftKeyboard()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycleScope.cancel()
    }

    inner class SongPopupTextProvider : PopupTextProvider {
        override fun getPopupText(position: Int): CharSequence {
            if (position != 0) {
                return filteredList[position - 1].mediaMetadata.title?.first().toString()
            }
            return "-"
        }
    }

}
