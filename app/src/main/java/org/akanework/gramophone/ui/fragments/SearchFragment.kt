package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.px
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.SongDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@androidx.annotation.OptIn(UnstableApi::class)
class SearchFragment : Fragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_search, container, false)
        val editText = rootView.findViewById<EditText>(R.id.edit_text)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val songAdapter = SongAdapter(mutableListOf(), requireActivity() as MainActivity)
        val songDecorAdapter =
            SongDecorAdapter(
                requireContext(),
                0,
                songAdapter,
            )
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)
        val returnButton = rootView.findViewById<MaterialButton>(R.id.return_button)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = concatAdapter

        FastScrollerBuilder(recyclerView).apply {
            setPadding(0, 0, 0, (66).px)
            build()
        }

        editText.addTextChangedListener {
            if (editText.text.isNullOrBlank()) {
                songAdapter.updateList(mutableListOf())
                songDecorAdapter.updateSongCounter(0)
            } else {
                CoroutineScope(Dispatchers.Default).launch {
                    val filteredList =
                        libraryViewModel.mediaItemList.value?.filter {
                            val isMatchingTitle = it.mediaMetadata.title!!.contains(editText.text, true)
                            val isMatchingAlbum =
                                it.mediaMetadata.albumTitle!!.contains(editText.text, true)
                            val isMatchingArtist =
                                it.mediaMetadata.artist!!.contains(editText.text, true)
                            isMatchingTitle || isMatchingAlbum || isMatchingArtist
                        }
                    if (filteredList != null) {
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
        }

        returnButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return rootView
    }
}
