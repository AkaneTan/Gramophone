package org.akanework.gramophone.logic.utils

import android.content.res.Resources

/**
 * This file contains some extension methods that made
 * For Gramophone.
 */
val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
