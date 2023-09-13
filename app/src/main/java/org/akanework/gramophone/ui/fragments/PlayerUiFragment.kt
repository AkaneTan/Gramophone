package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import org.akanework.gramophone.R

class PlayerUiFragment : PlayerFragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_player_ui, container, false)
		val popBackListener = requireActivity().onBackPressedDispatcher.addCallback(this) {
			childFragmentManager.popBackStack()
		}
		onCreateBottomSheet(rootView)
		childFragmentManager.addOnBackStackChangedListener {
			popBackListener.isEnabled = childFragmentManager.backStackEntryCount > 0
		}
		return rootView
	}
}