package org.akanework.serendipity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.serendipity.logic.utils.MediaStoreUtils
import org.akanework.serendipity.ui.viewmodels.LibraryViewModel

class MainActivity : AppCompatActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (libraryViewModel.mediaItemList.value!!.isEmpty()) {
            CoroutineScope(Dispatchers.Default).launch {
                val pairObject = MediaStoreUtils.getAllSongs(applicationContext)
                withContext(Dispatchers.Main) {
                    libraryViewModel.mediaItemList.value = pairObject.first
                    libraryViewModel.albumItemList.value = pairObject.second
                    libraryViewModel.artistItemList.value = pairObject.third
                }
            }
        }

        // Set content Views.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val fragmentContainer = findViewById<FragmentContainerView>(R.id.fragment_container)

    }
}