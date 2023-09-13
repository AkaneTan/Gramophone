package org.akanework.gramophone.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.R


class SettingsTopFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_top, rootKey)
        val versionPrefs = findPreference<Preference>("app_version")
        versionPrefs!!.summary = BuildConfig.VERSION_NAME
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(ColorDrawable(Color.TRANSPARENT))
    }

    override fun setDividerHeight(height: Int) {
        super.setDividerHeight(0)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == "dark_mode") {
            if ((preference as SwitchPreferenceCompat).isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}
