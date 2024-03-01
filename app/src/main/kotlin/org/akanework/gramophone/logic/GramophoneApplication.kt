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

package org.akanework.gramophone.logic

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.google.android.material.color.DynamicColors
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.logic.ui.BugHandlerActivity
import kotlin.system.exitProcess

/**
 * GramophoneApplication:
 *   We recover some configuration and apply dynamic color
 * here.
 *
 * @author AkaneTan, nift4
 */
class GramophoneApplication : Application() {

    lateinit var prefs: SharedPreferences
        private set

    override fun onCreate() {
        // Set up BugHandlerActivity.
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
            val exceptionMessage = Log.getStackTraceString(paramThrowable)
            val threadName = Thread.currentThread().name
            val intent = Intent(this, BugHandlerActivity::class.java)
            intent.putExtra("exception_message", exceptionMessage)
            intent.putExtra("thread", threadName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            exitProcess(10)
        }
        super.onCreate()
        // Cheat by loading preferences before setting up StrictMode.
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (BuildConfig.DEBUG) {
            // Use StrictMode to find anti-patterns issues (as of writing, no known violations)
            // (of course not counting SharedPreferences which just is like that by nature)
            StrictMode.setThreadPolicy(ThreadPolicy.Builder()
                .detectAll().permitDiskReads() // permit disk reads due to media3 setMetadata()
                .penaltyLog().penaltyDialog().build())
            StrictMode.setVmPolicy(VmPolicy.Builder()
                .detectAll()
                .penaltyLog().penaltyDeath().build())
        }

        // https://github.com/androidx/media/issues/805
        @OptIn(UnstableApi::class)
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
        }

        // Set application theme when launching.
        when (prefs.getStringStrict("theme_mode", "0")) {
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

        // Disk cache has been disabled for Gramophone, so clear out what's left of it
        // This should be removed in a few months
        Thread {
            Glide.get(applicationContext).clearDiskCache()
        }.start()
    }
}
