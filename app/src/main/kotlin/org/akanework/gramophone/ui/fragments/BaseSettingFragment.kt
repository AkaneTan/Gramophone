package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.ColorUtils

abstract class BaseSettingFragment(private val str: Int,
                                   private val fragmentCreator: () -> BasePreferenceFragment)
	: BaseFragment(false) {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_top_settings, container, false)
		val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

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
		collapsingToolbar.title = getString(str)

		topAppBar.setNavigationOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}

		childFragmentManager
			.beginTransaction()
			.addToBackStack(System.currentTimeMillis().toString())
			.add(R.id.settings, fragmentCreator())
			.commit()

		return rootView
	}
}