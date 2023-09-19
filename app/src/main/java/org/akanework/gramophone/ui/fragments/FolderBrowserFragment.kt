package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.FolderAdapter
import org.akanework.gramophone.ui.adapters.FolderPopAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@androidx.annotation.OptIn(UnstableApi::class)
class FolderBrowserFragment(val fileNode: MediaStoreUtils.FileNode? = null) : Fragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var songAdapter: SongAdapter
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val colorBackground = MaterialColors.getColor(view, android.R.attr.colorBackground)
        view.setBackgroundColor(colorBackground)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_folder_browser, container, false)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        if (fileNode == null) {
            folderAdapter = FolderAdapter(libraryViewModel.folderStructure.value!!.folderList
                .first().folderList
                .first().folderList
                .first().folderList, requireParentFragment().childFragmentManager)
            songAdapter = SongAdapter(libraryViewModel.folderStructure.value!!.folderList
                .first().folderList
                .first().folderList
                .first().songList,
                requireActivity() as MainActivity)
            concatAdapter = ConcatAdapter(folderAdapter, songAdapter)
        } else {
            folderAdapter = FolderAdapter(fileNode.folderList, requireParentFragment().childFragmentManager)
            songAdapter = SongAdapter(fileNode.songList, requireActivity() as MainActivity)
            concatAdapter = ConcatAdapter(FolderPopAdapter(requireParentFragment().childFragmentManager), folderAdapter, songAdapter)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = concatAdapter
        return rootView
    }

}
