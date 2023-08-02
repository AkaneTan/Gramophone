package org.akanework.serendipity

import android.app.Application
import androidx.activity.viewModels
import com.google.android.material.color.DynamicColors
import org.akanework.serendipity.ui.viewmodels.LibraryViewModel

class SerendipityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}