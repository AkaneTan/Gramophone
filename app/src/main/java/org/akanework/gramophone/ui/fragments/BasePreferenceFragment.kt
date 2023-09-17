package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.transition.MaterialSharedAxis

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
		returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
		exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
		reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
	}

}