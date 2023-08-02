package org.akanework.serendipity

import android.app.Application
import com.google.android.material.color.DynamicColors

class SerendipityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}