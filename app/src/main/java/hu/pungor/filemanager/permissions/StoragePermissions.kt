package hu.pungor.filemanager.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.loadFilesWithPermissionCheck

fun FileManagerActivity.checkPermissionsAndLoadFiles() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 1000)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 1000)
            }
        }
    } else
        loadFilesWithPermissionCheck()
}
