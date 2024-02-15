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

package org.akanework.gramophone

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import kotlin.system.exitProcess

/**
 * GramophoneApplication:
 *   We recover some configuration and apply dynamic color
 * here.
 *
 * @author AkaneTan, nift4
 */
class GramophoneApplication : Application() {
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // https://github.com/androidx/media/issues/805
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Set application theme when launching.
        when (prefs.getString("theme_mode", "0")) {
            "0" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            "1" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            "2" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Apply dynamic colors.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Set up BugHandlerActivity.
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
            val exceptionMessage = android.util.Log.getStackTraceString(paramThrowable)

            val intent = Intent(this, BugHandlerActivity::class.java)
            intent.putExtra("exception_message", exceptionMessage)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}
