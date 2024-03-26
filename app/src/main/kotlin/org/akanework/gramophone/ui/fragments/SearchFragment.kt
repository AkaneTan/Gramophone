/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.closeKeyboard
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.showKeyboard
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.ui.LibraryViewModel
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.SongAdapter

/**
 * SearchFragment:
 *   A fragment that contains a search bar which browses
 * the library finding items matching user input.
 *
 * @author AkaneTan
 */
class SearchFragment : BaseFragment(false) {
    private val handler = Handler(Looper.getMainLooper())
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val filteredList: MutableList<MediaItem> = mutableListOf()
    private lateinit var editText: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_search, container, false)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appbarlayout)
        appBarLayout.enableEdgeToEdgePaddingListener()
        editText = rootView.findViewById(R.id.edit_text)
        val recyclerView = rootView.findViewById<MyRecyclerView>(R.id.recyclerview)
        val songAdapter =
            SongAdapter(requireActivity() as MainActivity, listOf(),
                true, null, false, isSubFragment = true,
                allowDiffUtils = true, rawOrderExposed = true)
        val returnButton = rootView.findViewById<MaterialButton>(R.id.return_button)

        recyclerView.enableEdgeToEdgePaddingListener(ime = true)
        recyclerView.setAppBar(appBarLayout)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = songAdapter.concatAdapter

        // Build FastScroller.
        recyclerView.fastScroll(songAdapter, songAdapter.itemHeightHelper)

        editText.addTextChangedListener { rawText ->
            // TODO sort results by match quality? (using NaturalOrderHelper)
            if (rawText.isNullOrBlank()) {
                songAdapter.updateList(listOf(), now = true, true)
            } else {
                // make sure the user doesn't edit away our text while we are filtering
                val text = rawText.toString()
                // Launch a coroutine for searching in the library.
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    // Clear the list from the last search.
                    filteredList.clear()
                    // Filter the library.
                    libraryViewModel.mediaItemList.value?.filter {
                        val isMatchingTitle = it.mediaMetadata.title?.contains(text, true) ?: false
                        val isMatchingAlbum =
                            it.mediaMetadata.albumTitle?.contains(text, true) ?: false
                        val isMatchingArtist =
                            it.mediaMetadata.artist?.contains(text, true) ?: false
                        isMatchingTitle || isMatchingAlbum || isMatchingArtist
                    }?.let {
                        filteredList.addAll(
                            it
                        )
                    }
                    handler.post {
                        songAdapter.updateList(filteredList, now = true, true)
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
        if (!isHidden) {
            requireActivity().closeKeyboard(editText)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            requireActivity().showKeyboard(editText)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            requireActivity().closeKeyboard(editText)
        } else {
            requireActivity().showKeyboard(editText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycleScope.cancel()
    }

}
