/*
 *     Copyright (C) 2023  Akane Foundation
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.closeKeyboard
import org.akanework.gramophone.logic.showSoftKeyboard
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

/**
 * SearchFragment:
 *   A fragment that contains a search bar which browses
 * the library finding items matching user input.
 *
 * @author AkaneTan
 */
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
        val songAdapter =
            SongAdapter(requireActivity() as MainActivity, listOf(), false, null, false)
        val returnButton = rootView.findViewById<MaterialButton>(R.id.return_button)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appbarlayout)

        // Get our processed colors.
        val processColor = ColorUtils.getColor(
            MaterialColors.getColor(
                rootView,
                android.R.attr.colorBackground
            ),
            ColorUtils.ColorType.COLOR_BACKGROUND,
            requireContext(),
            true
        )

        // Overlap google's color with our color.
        rootView.setBackgroundColor(processColor)
        appBarLayout.setBackgroundColor(processColor)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = songAdapter.concatAdapter

        // Build FastScroller.
        FastScrollerBuilder(recyclerView).apply {
            setPopupTextProvider(songAdapter)
            useMd2Style()
            setTrackDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_transparent)!!)
            build()
        }

        editText.addTextChangedListener { text ->
            // TODO sort results by match quality?
            if (text.isNullOrBlank()) {
                songAdapter.updateList(listOf(), now = false, true)
            } else {
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
                    withContext(Dispatchers.Main) {
                        songAdapter.updateList(filteredList, now = false, true)
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

}
