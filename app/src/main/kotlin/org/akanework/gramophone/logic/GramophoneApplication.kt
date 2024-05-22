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
import android.media.ThumbnailUtils
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.preference.PreferenceManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.NullRequestDataException
import coil3.size.pxOrElse
import coil3.util.Logger
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.logic.ui.BugHandlerActivity
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

/**
 * GramophoneApplication
 *
 * @author AkaneTan, nift4
 */
class GramophoneApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        private const val TAG = "GramophoneApplication"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        // Set up BugHandlerActivity.
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
            val exceptionMessage = Log.getStackTraceString(paramThrowable)
            val threadName = Thread.currentThread().name
            Log.e(TAG, "Error on thread $threadName:\n $exceptionMessage")
            val intent = Intent(this, BugHandlerActivity::class.java)
            intent.putExtra("exception_message", exceptionMessage)
            intent.putExtra("thread", threadName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            exitProcess(10)
        }
        super.onCreate()
        // Cheat by loading preferences before setting up StrictMode.
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
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
        if (needsMissingOnDestroyCallWorkarounds()) {
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
    }

    @kotlin.OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCache(null)
            .components {
                if (hasScopedStorageV1()) {
                    add(Fetcher.Factory { data, options, _ ->
                        if (data !is Pair<*, *>) return@Factory null
                        val size = data.second
                        if (size !is Size?) return@Factory null
                        val file = data.first as? File ?: return@Factory null
                        return@Factory Fetcher {
                            ImageFetchResult(
                                ThumbnailUtils.createAudioThumbnail(file, options.size.let {
                                        Size(it.width.pxOrElse { size?.width ?: 10000 },
                                            it.height.pxOrElse { size?.height ?: 10000 })
                                    }, null).asCoilImage(), true, DataSource.DISK)
                        }
                    })
                }
            }
            .run {
                if (!BuildConfig.DEBUG) this else
                logger(object : Logger {
                    override var minLevel = Logger.Level.Verbose
                    override fun log(
                        tag: String,
                        level: Logger.Level,
                        message: String?,
                        throwable: Throwable?
                    ) {
                        if (level < minLevel) return
                        val priority = level.ordinal + 2 // obviously the best way to do it
                        if (message != null) {
                            Log.println(priority, tag, message)
                        }
                        // Let's keep the log readable and ignore normal events' stack traces.
                        if (throwable != null && throwable !is NullRequestDataException
                            && (throwable !is IOException
                                    || throwable.message != "No album art found")) {
                            Log.println(priority, tag, Log.getStackTraceString(throwable))
                        }
                    }
                })
            }
            .build()
    }
}
