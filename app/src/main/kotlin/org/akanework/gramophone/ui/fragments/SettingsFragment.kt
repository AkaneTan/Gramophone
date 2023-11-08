package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.ui.MainActivity

class SettingsFragment : BaseFragment(false) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

        val collapsingToolbar = rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingtoolbar)
        val processColor = ColorUtils.getColorBackground(
            MaterialColors.getColor(
                topAppBar,
                android.R.attr.colorBackground
            )
        )
        val processColorElevated = ColorUtils.getColorBackgroundElevated(
            MaterialColors.getColor(
                topAppBar,
                android.R.attr.colorBackground
            )
        )

        collapsingToolbar.setBackgroundColor(processColor)
        collapsingToolbar.setContentScrimColor(processColorElevated)

        topAppBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isLegacyProgressEnabled = prefs.getBoolean("default_progress_bar", false)
        val isContentBasedColorEnabled = prefs.getBoolean("content_based_color", true)
        val bottomSheetFullSlider = requireActivity().findViewById<Slider>(R.id.slider_vert)
        val bottomSheetFullSeekBar = requireActivity().findViewById<SeekBar>(R.id.slider_squiggly)
        if (isLegacyProgressEnabled) {
            bottomSheetFullSlider.visibility = View.VISIBLE
            bottomSheetFullSeekBar.visibility = View.GONE
        } else {
            bottomSheetFullSlider.visibility = View.GONE
            bottomSheetFullSeekBar.visibility = View.VISIBLE
        }
        if (!isContentBasedColorEnabled) {
            (activity as MainActivity).getPlayerSheet().removeColorScheme()
        } else {
            (activity as MainActivity).getPlayerSheet().addColorScheme()
        }
    }
}