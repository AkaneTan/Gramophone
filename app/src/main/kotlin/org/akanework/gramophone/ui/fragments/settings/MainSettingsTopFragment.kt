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

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.preference.Preference
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.BasePreferenceFragment

class MainSettingsTopFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_top, rootKey)
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(ColorDrawable(Color.TRANSPARENT))
    }

    override fun setDividerHeight(height: Int) {
        super.setDividerHeight(0)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "appearance" -> {
                val supportFragmentManager = requireActivity().supportFragmentManager
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(System.currentTimeMillis().toString())
                    .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                    .add(R.id.container, AppearanceSettingsFragment())
                    .commit()
            }
            "behavior" -> {
                val supportFragmentManager = requireActivity().supportFragmentManager
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(System.currentTimeMillis().toString())
                    .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                    .add(R.id.container, BehaviorSettingsFragment())
                    .commit()
            }
            "about" -> {
                val supportFragmentManager = requireActivity().supportFragmentManager
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(System.currentTimeMillis().toString())
                    .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                    .add(R.id.container, AboutSettingsFragment())
                    .commit()
            }
            "player" -> {
                val supportFragmentManager = requireActivity().supportFragmentManager
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(System.currentTimeMillis().toString())
                    .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                    .add(R.id.container, PlayerSettingsFragment())
                    .commit()
            }
            "audio" -> {
                val supportFragmentManager = requireActivity().supportFragmentManager
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(System.currentTimeMillis().toString())
                    .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                    .add(R.id.container, AudioSettingsFragment())
                    .commit()
            }
            "experimental" -> {
                val supportFragmentManager = requireActivity().supportFragmentManager
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(System.currentTimeMillis().toString())
                    .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                    .add(R.id.container, ExperimentalSettingsFragment())
                    .commit()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // nothing here
    }

}
