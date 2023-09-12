package org.akanework.gramophone.ui.components

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils.updateLibraryWithInCoroutine
import org.akanework.gramophone.ui.fragments.SearchFragment
import org.akanework.gramophone.ui.fragments.SettingsFragment
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel
import kotlin.random.Random

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MenuBottomSheet : BottomSheetDialogFragment() {

    val libraryViewModel: LibraryViewModel by activityViewModels()

    @SuppressLint("StringFormatMatches")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.modal_bottom_sheet, container, false)
        val refreshButton = rootView.findViewById<FrameLayout>(R.id.refresh)
        val settingsButton = rootView.findViewById<FrameLayout>(R.id.settings)
        val shuffleButton = rootView.findViewById<FrameLayout>(R.id.shuffle)
        val searchButton = rootView.findViewById<FrameLayout>(R.id.search)

        refreshButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                updateLibraryWithInCoroutine(libraryViewModel, requireContext())
                withContext(Dispatchers.Main) {
                    val snackBar =
                        Snackbar.make(
                            requireView(),
                            getString(
                                R.string.refreshed_songs,
                                libraryViewModel.mediaItemList.value?.size
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
                    snackBar.anchorView = view
                    snackBar.show()
                }
            }
        }

        settingsButton.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .addToBackStack("SETTINGS")
                .replace(R.id.container, SettingsFragment())
                .commit()
            this@MenuBottomSheet.dismiss()
        }

        shuffleButton.setOnClickListener {
            libraryViewModel.mediaItemList.value?.let { it1 ->
                val controller = (requireActivity() as MainActivity).getPlayer()
                controller.setMediaItems(it1)
                controller.seekToDefaultPosition(Random.nextInt(0, it1.size))
                controller.prepare()
                controller.play()
            }
            this@MenuBottomSheet.dismiss()
        }

        searchButton.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .addToBackStack("SEARCH")
                .replace(R.id.container, SearchFragment())
                .commit()
            this@MenuBottomSheet.dismiss()
        }

        return rootView
    }

}