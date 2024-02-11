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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter
import org.akanework.gramophone.ui.fragments.settings.MainSettingsFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

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

        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    (requireActivity() as MainActivity).startFragment(SearchFragment())
                }
                R.id.equalizer -> {
                    val intent = Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL")
                        .addCategory("android.intent.category.CATEGORY_CONTENT_MUSIC")
                    try {
                        (requireActivity() as MainActivity).startingActivity.launch(intent)
                    } catch (e: Exception) {
                        // Let's show a toast here if no system inbuilt EQ was found.
                        Toast.makeText(
                            requireContext(),
                            R.string.equalizer_not_found,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                R.id.refresh -> {
                    val playerLayout = (requireActivity() as MainActivity).getPlayerSheet()
                    CoroutineScope(Dispatchers.Default).launch {
                        MediaStoreUtils.updateLibraryWithInCoroutine(
                            libraryViewModel,
                            requireContext()
                        )

                        // Show a snack bar when updating is completed.
                        withContext(Dispatchers.Main) {

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
                            snackBar.show()
                        }
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
        val adapter = ViewPager2Adapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager2.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = getString(adapter.getLabelResId(position))
        }.attach()

        /*
         * Add margin to last and first tab.
         * There's no attribute to let you set margin
         * to the last tab.
         */
        val lastTab = tabLayout.getTabAt(tabLayout.tabCount - 1)!!.view
        val firstTab = tabLayout.getTabAt(0)!!.view
        val lastParam = lastTab.layoutParams as ViewGroup.MarginLayoutParams
        val firstParam = firstTab.layoutParams as ViewGroup.MarginLayoutParams
        lastParam.marginEnd = resources.getDimension(R.dimen.tab_layout_content_padding).toInt()
        firstParam.marginStart = resources.getDimension(R.dimen.tab_layout_content_padding).toInt()
        lastTab.layoutParams = lastParam
        firstTab.layoutParams = firstParam

        return rootView
    }
}
