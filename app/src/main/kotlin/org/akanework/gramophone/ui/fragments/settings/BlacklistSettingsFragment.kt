package org.akanework.gramophone.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.BlacklistFolderAdapter
import org.akanework.gramophone.ui.fragments.BaseFragment
import org.akanework.gramophone.ui.LibraryViewModel

class BlacklistSettingsFragment : BaseFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_blacklist_settings, container, false)
        val materialToolbar = rootView.findViewById<MaterialToolbar>(R.id.materialToolbar)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val folderArray = libraryViewModel.allFolderSet.value?.toMutableList() ?: mutableListOf()
        folderArray.sort()

        materialToolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = BlacklistFolderAdapter(folderArray, prefs)

        return rootView
    }
}