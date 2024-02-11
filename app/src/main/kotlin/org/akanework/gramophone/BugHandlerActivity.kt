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

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * BugHandlerActivity:
 *   An activity makes crash reporting easier.
 */
class BugHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug_handler)

        val bugText = findViewById<TextView>(R.id.error)
        val receivedText = intent.getStringExtra("exception_message")

        val deviceBrand = Build.BRAND
        val deviceModel = Build.MODEL
        val sdkLevel = Build.VERSION.SDK_INT
        val currentDateTime = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDateTime = formatter.format(currentDateTime)
        val gramophoneVersion = BuildConfig.VERSION_NAME

        val combinedTextBuilder = StringBuilder()
        combinedTextBuilder
            .append(getString(R.string.crash_gramophone_version)).append(':').append(' ').append(gramophoneVersion).append('\n').append('\n')
            .append(getString(R.string.crash_phone_brand)).append(':').append("     ").append(deviceBrand).append('\n')
            .append(getString(R.string.crash_phone_model)).append(':').append("     ").append(deviceModel).append('\n')
            .append(getString(R.string.crash_sdk_level)).append(':').append(' ').append(sdkLevel).append('\n').append('\n')
            .append(getString(R.string.crash_time)).append(':').append(' ').append(formattedDateTime).append('\n').append('\n')
            .append("--------- beginning of crash").append('\n')
            .append(receivedText)

        bugText.typeface = Typeface.MONOSPACE
        bugText.text = combinedTextBuilder.toString()

        // Make our life easier by copying the log to clipboard
        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("error msg", combinedTextBuilder.toString())
        clipboard.setPrimaryClip(clip)

    }
}