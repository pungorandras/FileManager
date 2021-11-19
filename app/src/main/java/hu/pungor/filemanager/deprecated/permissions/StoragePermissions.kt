package hu.pungor.filemanager.deprecated.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import hu.pungor.filemanager.deprecated.FileManagerActivity
import hu.pungor.filemanager.loadFilesWithPermissionCheck

class StoragePermissions {

    fun checkPermissionsAndLoadFiles(activity: FileManagerActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + activity.packageName)
                    )
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    activity.startActivity(intent)
                }
            }
            activity.loadFiles()
        } else
            activity.loadFilesWithPermissionCheck()
    }
}