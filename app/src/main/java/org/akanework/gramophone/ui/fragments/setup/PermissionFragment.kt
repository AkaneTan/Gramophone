package org.akanework.gramophone.ui.fragments.setup

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.checkEssentialPermission
import org.akanework.gramophone.ui.SetupActivity


class PermissionFragment : Fragment() {
    private lateinit var launcher: ActivityResultLauncher<Array<String>>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_setup_permission, container, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rootView.findViewById<View>(R.id.storage_permission).visibility = View.GONE
        } else {
            rootView.findViewById<View>(R.id.music_permission).visibility = View.GONE
            rootView.findViewById<View>(R.id.pic_permission).visibility = View.GONE
            rootView.findViewById<View>(R.id.notification_permission).visibility = View.GONE
        }
        launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val basePerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
            if (it[basePerm] == true) {
                (requireActivity() as SetupActivity).overrideContinueListener = null
            } else {
                AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.we_need_some_permissions)
                    .setMessage(R.string.permission_desc)
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        // do nothing
                    }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        try {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also { i ->
                                i.setData(Uri.parse("package:${requireContext().packageName}"))
                            })
                        } catch (e: ActivityNotFoundException) {
                            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
                        }
                    }
                    .show()
            }
        }
        for (i in listOf(R.id.storage_permission, R.id.music_permission, R.id.pic_permission,
            R.id.notification_permission)) {
            rootView.findViewById<View>(i).setOnClickListener { doLaunch() }
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (!requireContext().checkEssentialPermission()) {
            (requireActivity() as SetupActivity).overrideContinueListener = {
                doLaunch()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as SetupActivity).overrideContinueListener = null
    }

    fun doLaunch() {
        launcher.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            else
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
        )
    }
}