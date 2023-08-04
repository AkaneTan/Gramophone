package org.akanework.gramophone

import android.app.Application
import com.google.android.material.color.DynamicColors

class GramophoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}