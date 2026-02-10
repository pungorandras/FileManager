package hu.pungor.filemanager.permissions

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.loadFilesWithPermissionCheck

fun FileManagerActivity.checkPermissionsAndLoadFiles() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { _ ->
                loadFiles()
            }

            try {
                permissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        "package:$packageName".toUri()
                    )
                )
            } catch (_: Exception) {
                permissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else
            loadFiles()
    } else
        loadFilesWithPermissionCheck()
}
