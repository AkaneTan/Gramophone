/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.fragments.settings

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.px
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.fragments.BaseFragment

class MainSettingsFragment : BaseFragment(false) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_top_settings, container, false)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)

        val collapsingToolbar = rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingtoolbar)
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
        val isTitleCentered = prefs.getBoolean("centered_title", true)
        val activity = requireActivity()
        val bottomSheetFullSlider = activity.findViewById<Slider>(R.id.slider_vert)
        val bottomSheetFullSeekBar = activity.findViewById<SeekBar>(R.id.slider_squiggly)
        val bottomSheetFullTitle = activity.findViewById<TextView>(R.id.full_song_name)
        val bottomSheetFullSubtitle = activity.findViewById<TextView>(R.id.full_song_artist)
        val bottomSheetFullCoverFrame = activity.findViewById<MaterialCardView>(R.id.album_cover_frame)
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
        if (isTitleCentered) {
            bottomSheetFullTitle.gravity = Gravity.CENTER
            bottomSheetFullSubtitle.gravity = Gravity.CENTER
        } else {
            bottomSheetFullTitle.gravity = Gravity.CENTER_HORIZONTAL or Gravity.START
            bottomSheetFullSubtitle.gravity = Gravity.CENTER_HORIZONTAL or Gravity.START
        }
        bottomSheetFullCoverFrame.radius = prefs.getInt("album_round_corner", 22).px.toFloat()
    }
}