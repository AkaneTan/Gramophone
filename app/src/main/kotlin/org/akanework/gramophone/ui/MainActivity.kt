/*
 *     Copyright (C) 2024  Akane Foundation
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

package org.akanework.gramophone.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.ColorUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils.updateLibraryWithInCoroutine
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter.Companion.tabs
import org.akanework.gramophone.ui.components.PlayerBottomSheet
import org.akanework.gramophone.ui.fragments.BaseFragment
import org.akanework.gramophone.ui.fragments.SearchFragment
import org.akanework.gramophone.ui.fragments.settings.MainSettingsFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel
import kotlin.random.Random

/**
 * MainActivity:
 *   Core of gramophone, one and the only activity
 * used across the application.
 *
 * @author AkaneTan, nift4
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_READ_MEDIA_AUDIO = 100
        private const val PERMISSION_READ_EXTERNAL_STORAGE = 101
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE = 102
    }

    // Import our viewModels.
    private val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

    private lateinit var prefs: SharedPreferences

    /**
     * navigateDrawer:
     *   Used to navigate activity drawer in child fragments
     *
     * @param targetDrawer: Target drawer
     */
    fun navigateDrawer(targetDrawer: Int) {
        drawerLayout.open()
        navigationView.setCheckedItem(tabs[targetDrawer])
    }

    /**
     * updateLibrary:
     *   Calls [updateLibraryWithInCoroutine] in MediaStoreUtils and updates library.
     */
    private fun updateLibrary() {
        CoroutineScope(Dispatchers.Default).launch {
            updateLibraryWithInCoroutine(libraryViewModel, this@MainActivity)
        }
    }

    /**
     * onCreate - core of MainActivity.
     */
    @SuppressLint("StringFormatMatches", "StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // This is required for fragment navigate animation.
        ActivityCompat.postponeEnterTransition(this)

        // Set cutout modes if target system is newer than pie.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }

        // Set edge-to-edge contents. (Android L edge-to-edge is not supported by Gramophone.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)
                // this won't be called in case we show()/hide() so
                // we handle that case in BaseFragment
                if (f is BaseFragment && f.wantsPlayer != null) {
                    getPlayerSheet().visible = f.wantsPlayer
                }
            }
        }, false)

        // Set content Views.
        setContentView(R.layout.activity_main)
        val fragmentContainerView: FragmentContainerView = findViewById(R.id.container)
        val coordinatorLayout = findViewById<CoordinatorLayout>(R.id.coordinatorLayout)

        // Initialize layouts.
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Process our own color here.
        val processColor = ColorUtils.getColor(
            MaterialColors.getColor(
                coordinatorLayout,
                android.R.attr.colorBackground
            ),
            ColorUtils.ColorType.COLOR_BACKGROUND,
            this,
            true
        )

        val colorSurfaceContainer = ColorUtils.getColor(
            MaterialColors.getColor(
                fragmentContainerView,
                com.google.android.material.R.attr.colorSurfaceContainer
            ),
            ColorUtils.ColorType.COLOR_BACKGROUND_TINTED,
            this,
            true
        )

        // Override google's colors.
        coordinatorLayout.setBackgroundColor(processColor)
        drawerLayout.setBackgroundColor(processColor)
        navigationView.setBackgroundColor(processColor)

        val previewPlayer = findViewById<ConstraintLayout>(R.id.preview_player)
        previewPlayer.setBackgroundColor(colorSurfaceContainer)
        previewPlayer.backgroundTintList =
            ColorStateList.valueOf(colorSurfaceContainer)
        getPlayerSheet().setBackgroundColor(colorSurfaceContainer)
        getPlayerSheet().backgroundTintList =
            ColorStateList.valueOf(colorSurfaceContainer)

        // Adjust insets so that bottom sheet can look more normal.
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainerView) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val notchInset = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            fragmentContainerView.setPadding(navBarInset.left + notchInset.left, 0,
                navBarInset.right + notchInset.right, notchInset.bottom)
            view.onApplyWindowInsets(insets.toWindowInsets())
            return@setOnApplyWindowInsetsListener insets
        }

        // Also adjust drawer's insets.
        ViewCompat.setOnApplyWindowInsetsListener(navigationView) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            navigationView.setPadding(navBarInset.left, 0, 0, navBarInset.bottom)
            view.onApplyWindowInsets(insets.toWindowInsets())
            return@setOnApplyWindowInsetsListener insets
        }

        // Check all permissions.
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED)
            || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                    && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED)
            || (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            // Ask if was denied.
            ActivityCompat.requestPermissions(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
                else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                else
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                PERMISSION_READ_MEDIA_AUDIO,
            )
        } else {
            // If all permissions are granted, we can update library now.
            if (libraryViewModel.mediaItemList.value!!.isEmpty()) {
                updateLibrary()
            }
        }

        // Bind navigationView's behavior.
        navigationView.setNavigationItemSelectedListener {
            val viewPager2 = fragmentContainerView.findViewById<ViewPager2>(R.id.fragment_viewpager)
            val playerLayout = getPlayerSheet()
            when (it.itemId) {
                in tabs -> {
                    viewPager2.setCurrentItem(
                        tabs.indices
                            .find { entry -> tabs[entry] == it.itemId }!!, true
                    )
                    drawerLayout.close()
                }

                R.id.refresh -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        updateLibraryWithInCoroutine(libraryViewModel, applicationContext)

                        // Show a snack bar when updating is completed.
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

                            /*
                             * Let's override snack bar's color here so it would
                             * adapt dark mode.
                             */
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

                            // Set an anchor for snack bar.
                            if (playerLayout.visible && playerLayout.actuallyVisible)
                                snackBar.anchorView = playerLayout
                            snackBar.show()
                        }
                    }
                    drawerLayout.close()
                }

                R.id.settings -> {
                    startFragment(MainSettingsFragment())
                }

                R.id.search -> {
                    startFragment(SearchFragment())
                }

                R.id.shuffle -> {
                    // Call shuffle method.
                    shuffle()
                }

                R.id.equalizer -> {
                    // Start system EQ here.
                    val intent = Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL")
                        .addCategory("android.intent.category.CATEGORY_CONTENT_MUSIC")
                    try {
                        startActivity.launch(intent)
                    } catch (e: Exception) {
                        // Let's show a toast here if no system inbuilt EQ was found.
                        Toast.makeText(
                            applicationContext,
                            R.string.equalizer_not_found,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                R.id.about -> {
                    // Display a card here.
                    val drawable = GradientDrawable()
                    drawable.color =
                        ColorStateList.valueOf(processColor)
                    drawable.cornerRadius = 64f
                    val rootView = MaterialAlertDialogBuilder(this)
                        .setBackground(drawable)
                        .setView(R.layout.dialog_about)
                        .show()
                    val versionTextView = rootView.findViewById<TextView>(R.id.version)!!
                    versionTextView.text =
                        BuildConfig.VERSION_NAME
                }

                else -> throw IllegalStateException()
            }
            true
        }
    }

    /**
     * onRequestPermissionResult:
     *   Update library after permission is granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_READ_MEDIA_AUDIO -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }

            PERMISSION_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }

            PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    updateLibrary()
                } else {
                    // TODO:  Show a prompt here
                }
            }
        }
    }

    /**
     * shuffle:
     *   Called by child fragment / drawer. It calls
     * controller's shuffle method.
     */
    fun shuffle() {
        libraryViewModel.mediaItemList.value?.takeIf { it.isNotEmpty() }?.let { it1 ->
            val controller = getPlayer()
            controller.setMediaItems(it1)
            controller.shuffleModeEnabled = true
            controller.seekToDefaultPosition(Random.nextInt(0, it1.size))
            controller.prepare()
            controller.play()
        }
    }

    /**
     * startFragment:
     *   Used by child fragments / drawer to start
     * a fragment inside MainActivity's fragment
     * scope.
     *
     * @param frag: Target fragment.
     */
    fun startFragment(frag: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .addToBackStack(System.currentTimeMillis().toString())
            .hide(supportFragmentManager.fragments.let { it[it.size - 1] })
            .add(R.id.container, frag)
            .commit()

        Handler(Looper.getMainLooper()).postDelayed({
            drawerLayout.close()
        }, 50)
    }

    /**
     * getPlayerSheet:
     *   Used by child fragment to get player sheet's
     *  view. Notice this would always return a non-null
     *  object since player layout is fixed in main
     *  activity's layout.
     */
    fun getPlayerSheet(): PlayerBottomSheet = findViewById(R.id.player_layout)

    /**
     * getPlayer:
     *   Returns a media controller.
     */
    fun getPlayer() = getPlayerSheet().getPlayer()

    /**
     * getPreferences:
     *   Returns a SharedPreference.
     */
    fun getPreferences() = prefs
}
