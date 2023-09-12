package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.transition.MaterialSharedAxis

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
		returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
		exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
		reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
	}

}