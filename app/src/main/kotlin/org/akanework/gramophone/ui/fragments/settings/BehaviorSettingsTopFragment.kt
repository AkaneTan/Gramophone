/*
 *     Copyright (C) 2024 Akane Foundation
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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.hasScopedStorageWithMediaTypes
import org.akanework.gramophone.ui.fragments.BasePreferenceFragment
import org.akanework.gramophone.ui.fragments.BaseSettingFragment


class BehaviorSettingsFragment : BaseSettingFragment(R.string.settings_category_behavior,
    { BehaviorSettingsTopFragment() })

class BehaviorSettingsTopFragment : BasePreferenceFragment() {
    override fun onResume() {
        super.onResume()
        if (hasScopedStorageWithMediaTypes()) {
            val preference = findPreference<SwitchPreferenceCompat>("album_covers")!!
            preference.isPersistent = false
            preference.isChecked = requireContext().checkSelfPermission(
                    android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_behavior, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == "blacklist") {
            val supportFragmentManager = requireActivity().supportFragmentManager
            supportFragmentManager
                .beginTransaction()
                .addToBackStack(System.currentTimeMillis().toString())
                .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
                .add(R.id.container, BlacklistSettingsFragment())
                .commit()
        }
        // Prior to Android 13, this changes a setting which changes MediaStoreUtils behaviour
        // Android 13 and later, this displays state of images permission granted/denied
        if (hasScopedStorageWithMediaTypes() && preference.key == "album_covers") {
            Toast.makeText(requireActivity(), if (requireContext().checkSelfPermission(
                    android.Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED) R.string.deny_images else
                    R.string.grant_images, Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.setData(Uri.parse("package:${requireContext().packageName}"))
            startActivity(intent)
        }
        return super.onPreferenceTreeClick(preference)
    }
}
