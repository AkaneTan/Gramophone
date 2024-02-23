package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.akanework.gramophone.R

abstract class BaseSettingFragment(private val titleString: Int,
                                   private val fragmentCreator: () -> BasePreferenceFragment)
	: BaseFragment(false) {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_top_settings, container, false)

		val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appBarLayout)
		val collapsingToolbar = rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
		val materialToolbar = rootView.findViewById<MaterialToolbar>(R.id.materialToolbar)
		val fragmentContainerView = rootView.findViewById<FragmentContainerView>(R.id.settings)

		// https://github.com/material-components/material-components-android/issues/1310
		ViewCompat.setOnApplyWindowInsetsListener(collapsingToolbar, null)

		ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
			val inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
				insets.getInsets(WindowInsetsCompat.Type.displayCutout()))
			v.updatePadding(inset.left, inset.top, inset.right, 0)
			return@setOnApplyWindowInsetsListener insets
		}
		ViewCompat.setOnApplyWindowInsetsListener(fragmentContainerView) { v, insets ->
			val inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
				insets.getInsets(WindowInsetsCompat.Type.displayCutout()))
			v.updatePadding(inset.left, 0, inset.right, inset.bottom)
			return@setOnApplyWindowInsetsListener insets
		}

		collapsingToolbar.title = getString(titleString)
		materialToolbar.setNavigationOnClickListener {
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