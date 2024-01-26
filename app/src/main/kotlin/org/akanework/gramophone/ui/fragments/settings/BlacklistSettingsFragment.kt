package org.akanework.gramophone.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.ui.adapters.BlacklistFolderAdapter
import org.akanework.gramophone.ui.fragments.BaseFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

class BlacklistSettingsFragment : BaseFragment() {

    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_blacklist_settings, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val collapsingToolbar =
            rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingtoolbar)
        val processColor = ColorUtils.getColor(
            MaterialColors.getColor(
                topAppBar,
                android.R.attr.colorBackground
            ),
            ColorUtils.ColorType.COLOR_BACKGROUND,
            requireContext(),
            true
        )
        val processColorElevated = ColorUtils.getColor(
            MaterialColors.getColor(
                topAppBar,
                android.R.attr.colorBackground
            ),
            ColorUtils.ColorType.TOOLBAR_ELEVATED,
            requireContext(),
            true
        )

        collapsingToolbar.setBackgroundColor(processColor)
        collapsingToolbar.setContentScrimColor(processColorElevated)


        val folderArray = libraryViewModel.allFolderSet.value?.toMutableList() ?: mutableListOf()
        folderArray.sort()

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val blacklistFolderAdapter = BlacklistFolderAdapter(folderArray, prefs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = blacklistFolderAdapter

        return rootView
    }
}