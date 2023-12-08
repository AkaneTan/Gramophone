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

package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.logic.utils.ColorUtils

/**
 * BasePreferenceFragment:
 *   A base fragment for all SettingsTopFragment. It
 * is used to make overlapping color easier.
 *
 * @author AkaneTan
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Overlap google's colors.
        val colorBackground = ColorUtils.getColor(
            MaterialColors.getColor(view, android.R.attr.colorBackground),
            ColorUtils.ColorType.COLOR_BACKGROUND,
            requireContext(),
            true
        )
        view.setBackgroundColor(colorBackground)
    }

}