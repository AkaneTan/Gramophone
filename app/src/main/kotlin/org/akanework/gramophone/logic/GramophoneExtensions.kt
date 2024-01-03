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

package org.akanework.gramophone.logic

import android.app.Activity
import android.content.Context
import android.content.res.Resources.getSystem
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.net.toFile
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import org.akanework.gramophone.logic.GramophonePlaybackService.Companion.SERVICE_GET_LYRICS
import org.akanework.gramophone.logic.GramophonePlaybackService.Companion.SERVICE_QUERY_TIMER
import org.akanework.gramophone.logic.GramophonePlaybackService.Companion.SERVICE_SET_TIMER
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import java.io.File

fun MediaController.playOrPause() {
    if (isPlaying) {
        pause()
    } else {
        play()
    }
}

fun MediaItem.getUri(): Uri? {
    return localConfiguration?.uri
}

fun MediaItem.getFile(): File? {
    return getUri()?.toFile()
}

fun Activity.closeKeyboard() {
    if (currentFocus != null) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
    }
}

fun View.showSoftKeyboard() {
    if (requestFocus()) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun Drawable.startAnimation() {
    when (this) {
        is AnimatedVectorDrawable -> start()
        is AnimatedVectorDrawableCompat -> start()
        else -> throw IllegalArgumentException()
    }
}

fun TextView.setTextAnimation(
    text: CharSequence?,
    duration: Long = 300,
    completion: (() -> Unit)? = null
) {
    if (this.text != text) {
        fadOutAnimation(duration) {
            this.text = text
            fadInAnimation(duration) {
                completion?.let {
                    it()
                }
            }
        }
    }
}

// ViewExtensions

fun View.fadOutAnimation(
    duration: Long = 300,
    visibility: Int = View.INVISIBLE,
    completion: (() -> Unit)? = null
) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = visibility
            completion?.let {
                it()
            }
        }
}

fun View.fadInAnimation(duration: Long = 300, completion: (() -> Unit)? = null) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .withEndAction {
            completion?.let {
                it()
            }
        }
}

val Int.dp: Int get() = (this.toFloat().dp).toInt()

val Int.px: Int get() = (this.toFloat().px).toInt()

val Float.dp: Float get() = this / getSystem().displayMetrics.density

val Float.px: Float get() = this * getSystem().displayMetrics.density

fun MediaController.getTimer(): Int =
    sendCustomCommand(
        SessionCommand(SERVICE_QUERY_TIMER, Bundle.EMPTY),
        Bundle.EMPTY
    ).get().extras.getInt("duration")

fun MediaController.hasTimer(): Boolean = getTimer() > 0
fun MediaController.setTimer(value: Int) {
    sendCustomCommand(
        SessionCommand(SERVICE_SET_TIMER, Bundle.EMPTY).apply {
            customExtras.putInt("duration", value)
        }, Bundle.EMPTY
    )
}

fun MediaController.getLyrics(): Array<MediaStoreUtils.Lyric>? =
    sendCustomCommand(
        SessionCommand(SERVICE_GET_LYRICS, Bundle.EMPTY),
        Bundle.EMPTY
    ).get().extras.let {
        @Suppress("UNCHECKED_CAST")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            it.getParcelableArray("lyrics", MediaStoreUtils.Lyric::class.java)
        } else {
            @Suppress("deprecation")
            it.getParcelableArray("lyrics") as Array<MediaStoreUtils.Lyric>
        }
    }