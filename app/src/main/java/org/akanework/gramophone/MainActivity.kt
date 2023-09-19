package org.akanework.gramophone

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.utils.MediaStoreUtils.updateLibraryWithInCoroutine
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter.Companion.tabs
import org.akanework.gramophone.ui.components.PlayerBottomSheet
import org.akanework.gramophone.ui.fragments.BaseFragment
import org.akanework.gramophone.ui.fragments.SearchFragment
import org.akanework.gramophone.ui.fragments.SettingsFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    
    // Import our viewModels.
    private val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    fun navigateDrawer(int: Int) {
        drawerLayout.open()
        navigationView.setCheckedItem(tabs[int])
    }

    private fun updateLibrary() {
        CoroutineScope(Dispatchers.Default).launch {
            updateLibraryWithInCoroutine(libraryViewModel, this@MainActivity)

        }
    }

    @SuppressLint("StringFormatMatches")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
            val exceptionMessage = android.util.Log.getStackTraceString(paramThrowable)

            val intent = Intent(this, BugHandlerActivity::class.java).setAction("bug_handle")
            intent.putExtra("exception_message", exceptionMessage)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkModeEnabled = prefs.getBoolean("dark_mode", false)
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        ActivityCompat.postponeEnterTransition(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)
                if (f is BaseFragment) {
                    getPlayerSheet().visible = f.wantsPlayer
                }
            }
        }, false)

        // Set content Views.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask if was denied.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                    Constants.PERMISSION_READ_MEDIA_AUDIO,
                )
            } else {
                if (libraryViewModel.mediaItemList.value!!.isEmpty()) {
                    updateLibrary()
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask if was denied.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.PERMISSION_READ_EXTERNAL_STORAGE,
                )
            } else {
                if (libraryViewModel.mediaItemList.value!!.isEmpty()) {
                    updateLibrary()
                }
            }
        }

        // Initialize layouts.
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        val fragmentContainerView: FragmentContainerView = findViewById(R.id.container)

        navigationView.setNavigationItemSelectedListener {
            val viewPager2 = fragmentContainerView.findViewById<ViewPager2>(R.id.fragment_viewpager)
            val playerLayout = getPlayerSheet()
            when (it.itemId) {
                in tabs -> {
                    viewPager2.setCurrentItem(tabs.indices
                        .find { entry -> tabs[entry] == it.itemId }!!, true)
                    drawerLayout.close()
                }

                R.id.refresh -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        updateLibraryWithInCoroutine(libraryViewModel, applicationContext)
                        withContext(Dispatchers.Main) {
                            val snackBar =
                                Snackbar.make(
                                    fragmentContainerView,
                                    getString(
                                        R.string.refreshed_songs,
                                        libraryViewModel.mediaItemList.value!!.size,
                                    ),
                                    Snackbar.LENGTH_LONG,
                                )
                            snackBar.setAction(R.string.dismiss) {
                                snackBar.dismiss()
                            }
                            snackBar.setBackgroundTint(
                                MaterialColors.getColor(
                                    snackBar.view,
                                    com.google.android.material.R.attr.colorSurface,
                                ),
                            )
                            snackBar.setActionTextColor(
                                MaterialColors.getColor(
                                    snackBar.view,
                                    com.google.android.material.R.attr.colorPrimary,
                                ),
                            )
                            snackBar.setTextColor(
                                MaterialColors.getColor(
                                    snackBar.view,
                                    com.google.android.material.R.attr.colorOnSurface,
                                ),
                            )
                            snackBar.anchorView = playerLayout
                            snackBar.show()
                        }
                    }
                    drawerLayout.close()
                }

                R.id.settings -> {
                    supportFragmentManager
                        .beginTransaction()
                        .addToBackStack("SETTINGS")
                        .replace(R.id.container, SettingsFragment())
                        .commit()
                    Handler(Looper.getMainLooper()).postDelayed({
                        drawerLayout.close()
                    }, 50)
                }

                R.id.search -> {
                    supportFragmentManager
                        .beginTransaction()
                        .addToBackStack("SEARCH")
                        .replace(R.id.container, SearchFragment())
                        .commit()
                    Handler(Looper.getMainLooper()).postDelayed({
                        drawerLayout.close()
                    }, 50)
                }

                R.id.shuffle -> {
                    libraryViewModel.mediaItemList.value?.let { it1 ->
                        val controller = getPlayer()
                        controller.setMediaItems(it1)
                        controller.shuffleModeEnabled = true
                        controller.prepare()
                        controller.play()
                    }
                }

                else -> throw IllegalStateException()
            }
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.PERMISSION_READ_MEDIA_AUDIO -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }

            Constants.PERMISSION_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }
        }
    }

    private fun getPlayerSheet() = findViewById<PlayerBottomSheet>(R.id.player_layout)
    fun getPlayer() = getPlayerSheet().getPlayer()
}
