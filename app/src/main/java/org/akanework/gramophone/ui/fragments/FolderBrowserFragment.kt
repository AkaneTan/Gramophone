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
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.FolderAdapter
import org.akanework.gramophone.ui.adapters.FolderPopAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

@androidx.annotation.OptIn(UnstableApi::class)
class FolderBrowserFragment(private val fileNode: MediaStoreUtils.FileNode? = null)
    : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var songAdapter: SongAdapter
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_folder_browser, container, false)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        if (fileNode == null) {
            val root = libraryViewModel.folderStructure.value!!.folderList
                .firstOrNull()?.folderList
                ?.firstOrNull()?.folderList
                ?.firstOrNull()
            folderAdapter = FolderAdapter(root?.folderList ?: mutableListOf(), requireParentFragment().childFragmentManager)
            songAdapter = SongAdapter(requireActivity() as MainActivity, root?.songList ?: mutableListOf(), false)
            concatAdapter = ConcatAdapter(folderAdapter, songAdapter)
            libraryViewModel.folderStructure.observe(viewLifecycleOwner) {
                val newRoot = libraryViewModel.folderStructure.value!!.folderList
                    .firstOrNull()?.folderList
                    ?.firstOrNull()?.folderList
                    ?.firstOrNull()
                folderAdapter.updateList(newRoot?.folderList ?: mutableListOf())
                songAdapter.updateList(newRoot?.songList ?: mutableListOf())
            }
        } else {
            folderAdapter = FolderAdapter(fileNode.folderList, requireParentFragment().childFragmentManager)
            songAdapter = SongAdapter(requireActivity() as MainActivity, fileNode.songList, false)
            concatAdapter = ConcatAdapter(FolderPopAdapter(requireParentFragment().childFragmentManager), folderAdapter, songAdapter)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = concatAdapter
        return rootView
    }

}
