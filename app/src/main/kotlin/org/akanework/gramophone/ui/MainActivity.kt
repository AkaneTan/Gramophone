package org.akanework.gramophone.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import org.akanework.gramophone.ui.fragments.SettingsFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_READ_MEDIA_AUDIO = 100
        private const val PERMISSION_READ_EXTERNAL_STORAGE = 101
    }

    // Import our viewModels.
    val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

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
        ActivityCompat.postponeEnterTransition(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
        val rootContainer = findViewById<View>(R.id.rootContainer)
        val coordinatorLayout = findViewById<CoordinatorLayout>(R.id.coordinatorLayout)

        val processColor = ColorUtils.getColor(
            MaterialColors.getColor(
                coordinatorLayout,
                android.R.attr.colorBackground
            ),
            ColorUtils.ColorType.COLOR_BACKGROUND,
            this
        )
        coordinatorLayout.setBackgroundColor(processColor)

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            rootContainer.setPadding(navBarInset.left, 0, navBarInset.right, 0)
            view.onApplyWindowInsets(insets.toWindowInsets())
            return@setOnApplyWindowInsetsListener insets
        }

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
                    PERMISSION_READ_MEDIA_AUDIO,
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
                    PERMISSION_READ_EXTERNAL_STORAGE,
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

        ViewCompat.setOnApplyWindowInsetsListener(navigationView) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            navigationView.setPadding(navBarInset.left, 0, 0, navBarInset.bottom)
            view.onApplyWindowInsets(insets.toWindowInsets())
            return@setOnApplyWindowInsetsListener insets
        }
        navigationView.setNavigationItemSelectedListener {
            val viewPager2 = fragmentContainerView.findViewById<ViewPager2>(R.id.fragment_viewpager)
            val playerLayout = getPlayerSheet()
            when (it.itemId) {
                in tabs -> {
                    viewPager2.setCurrentItem(tabs.indices
                        .find { entry -> tabs[entry] == it.itemId }!!, true
                    )
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
                            if (playerLayout.visible && playerLayout.actuallyVisible)
                                snackBar.anchorView = playerLayout
                            snackBar.show()
                        }
                    }
                    drawerLayout.close()
                }

                R.id.settings -> {
                    startFragment(SettingsFragment())
                }

                R.id.search -> {
                    startFragment(SearchFragment())
                }

                R.id.shuffle -> {
                    shuffle()
                }

                R.id.equalizer -> {
                    val intent = Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL")
                        .addCategory("android.intent.category.CATEGORY_CONTENT_MUSIC")
                    try {
                        startActivity.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            applicationContext,
                            R.string.equalizer_not_found,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                R.id.about -> {
                    val rootView = MaterialAlertDialogBuilder(this)
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
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    updateLibrary()
                } else {
                    // TODO: Show a prompt here
                }
            }
        }
    }

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

    fun getPlayerSheet(): PlayerBottomSheet = findViewById(R.id.player_layout)
    fun getPlayer() = getPlayerSheet().getPlayer()
}
