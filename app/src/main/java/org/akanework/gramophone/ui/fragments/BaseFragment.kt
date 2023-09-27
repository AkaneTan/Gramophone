package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.Slide
import com.google.android.material.color.MaterialColors

abstract class BaseFragment(val wantsPlayer: Boolean? = null) : Fragment() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		enterTransition = Slide(Gravity.END)
		returnTransition = Slide(Gravity.END)
		exitTransition = Slide(Gravity.START)
		reenterTransition = Slide(Gravity.START)
	}

	// https://github.com/material-components/material-components-android/issues/1984#issuecomment-1089710991
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val colorBackground = MaterialColors.getColor(view, android.R.attr.colorBackground)
		view.setBackgroundColor(colorBackground)
	}
}