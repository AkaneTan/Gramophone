package org.akanework.gramophone.logic.utils

import android.content.Context
import android.content.res.Configuration

object EnvUtils {

    fun isDarkMode(context: Context): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

}