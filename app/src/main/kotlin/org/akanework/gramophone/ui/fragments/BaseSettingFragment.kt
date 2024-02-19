package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.akanework.gramophone.R

abstract class BaseSettingFragment(private val str: Int,
                                   private val fragmentCreator: () -> BasePreferenceFragment)
	: BaseFragment(false) {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_top_settings, container, false)
		if (rootView !is ViewGroup)
			throw IllegalArgumentException()
		rootView.clipToPadding = false
		ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
			val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
			v.setPadding(0, 0, 0, navBarInset.bottom)
			v.onApplyWindowInsets(insets.toWindowInsets())
			return@setOnApplyWindowInsetsListener insets
		}
		val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

		val collapsingToolbar =
			rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingtoolbar)

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