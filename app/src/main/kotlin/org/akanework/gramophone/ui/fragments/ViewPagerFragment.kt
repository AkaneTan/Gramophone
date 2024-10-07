/*
 *     Copyright (C) 2024 Akane Foundation
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

package org.akanework.gramophone.ui.fragments

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.clone
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.getSessionId
import org.akanework.gramophone.logic.needsManualSnackBarInset
import org.akanework.gramophone.logic.updateMargin
import org.akanework.gramophone.ui.LibraryViewModel
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter
import org.akanework.gramophone.ui.fragments.settings.MainSettingsFragment

/**
 * ViewPagerFragment:
 *   A fragment that's in charge of displaying tabs
 * and is connected to the drawer.
 *
 * @author AkaneTan
 */
@androidx.annotation.OptIn(UnstableApi::class)
class ViewPagerFragment : BaseFragment(true) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    lateinit var appBarLayout: AppBarLayout
        private set

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_viewpager, container, false)
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tab_layout)
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val viewPager2 = rootView.findViewById<ViewPager2>(R.id.fragment_viewpager)

        appBarLayout = rootView.findViewById(R.id.appbarlayout)
        appBarLayout.enableEdgeToEdgePaddingListener()
        topAppBar.overflowIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_more_vert_alt)

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    (requireActivity() as MainActivity).startFragment(SearchFragment())
                }
                R.id.equalizer -> {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        // EXTRA_PACKAGE_NAME is probably not needed but might as well add for good measure
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, (requireActivity() as MainActivity).getPlayer()?.getSessionId())
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    try {
                        (requireActivity() as MainActivity).startingActivity.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        // Let's show a toast here if no system inbuilt EQ was found.
                        Toast.makeText(
                            requireContext(),
                            R.string.equalizer_not_found,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                R.id.refresh -> {
                    val activity = requireActivity() as MainActivity
                    val playerLayout = activity.playerBottomSheet
                    activity.updateLibrary {
                        val snackBar =
                            Snackbar.make(
                                requireView(),
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
                        else if (needsManualSnackBarInset()) {
                            // snack bar only implements proper insets handling for Q+
                            snackBar.view.updateMargin {
                                val i = ViewCompat.getRootWindowInsets(activity.window.decorView)
                                if (i != null) {
                                    bottom += i.clone()
                                        .getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                                }
                            }
                        }
                        snackBar.show()
                    }
                }
                R.id.settings -> {
                    (requireActivity() as MainActivity).startFragment(MainSettingsFragment())
                }

                else -> throw IllegalStateException()
            }
            true
        }

        // Connect ViewPager2.

        // Set this to 9999 so it won't lag anymore.
        viewPager2.offscreenPageLimit = 9999
        val adapter = ViewPager2Adapter(childFragmentManager, viewLifecycleOwner.lifecycle, requireContext(), viewPager2)
        viewPager2.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = getString(adapter.getLabelResId(position))
            tab.view.post {
                /*
                 * Add margin to last and first tab.
                 * There's no attribute to let you set margin
                 * to the last tab.
                 */
                val lp = tab.view.layoutParams as ViewGroup.MarginLayoutParams
                lp.marginStart = if (position == 0)
                    resources.getDimension(R.dimen.tab_layout_content_padding).toInt() else 0
                lp.marginEnd = if (position == tabLayout.tabCount - 1)
                    resources.getDimension(R.dimen.tab_layout_content_padding).toInt() else 0
                tab.view.layoutParams = lp
            }
        }.attach()

        return rootView
    }
}
