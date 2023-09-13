package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.FragmentManager
import org.akanework.gramophone.R

class PlayerUiFragment : PlayerFragment(), FragmentManager.OnBackStackChangedListener {
	private lateinit var popBackListener: OnBackPressedCallback
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_player_ui, container, false)
		popBackListener = requireActivity().onBackPressedDispatcher.addCallback(this) {
			childFragmentManager.popBackStack()
		}
		onCreateBottomSheet(rootView)
		childFragmentManager.addOnBackStackChangedListener(this)
		return rootView
	}

	override fun onBackStackChanged() {
		popBackListener.isEnabled = childFragmentManager.backStackEntryCount > 0
	}

	override fun onDestroyView() {
		childFragmentManager.removeOnBackStackChangedListener(this)
		super.onDestroyView()
	}
}