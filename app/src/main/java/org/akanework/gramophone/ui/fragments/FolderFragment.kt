package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.akanework.gramophone.R

class FolderFragment : BaseFragment(null) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_folder, container, false)
        childFragmentManager
            .beginTransaction()
            .addToBackStack("BROWSABLE")
            .replace(R.id.browser, FolderBrowserFragment())
            .commit()
        return rootView
    }

}
