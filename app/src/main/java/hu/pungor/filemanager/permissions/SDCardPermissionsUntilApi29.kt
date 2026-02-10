package hu.pungor.filemanager.permissions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alertDialogBuilder
import java.io.File
import java.util.regex.Pattern

fun FileManagerActivity.getSDCardPath(): File? {
    val pattern = Pattern.compile("([A-Z]|[0-9]){4}-([A-Z]|[0-9]){4}")
    val regex = pattern.toRegex()

    if (vcIsR) {
        for (element in externalMediaDirs) {
            val path = regex.find(element.path)
            if (path != null) {
                return File("/storage/" + path.value)
            }
        }
    } else if ((getUri() == null)) {
        sdCardPermissionsBuilder()
    } else
        return File("/storage/" + getUri()?.path?.let { regex.find(it)?.value })

    return null
}

fun FileManagerActivity.getUri(): Uri? {
    val persistedUriPermissions = contentResolver.persistedUriPermissions
    if (persistedUriPermissions.size > 0) {
        val uriPermission = persistedUriPermissions[0]
        return uriPermission.uri
    }
    return null
}

@SuppressLint("InflateParams")
fun FileManagerActivity.sdCardPermissionsBuilder() {
    val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            grantRWPermissions(result.data)
        }
    }

    alertDialogBuilder(
        titleText = R.string.info,
        dialogText = R.string.sdcard_permission,
        positiveButtonFunctionality = {
            permissionLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
        }
    ).show()
}

fun FileManagerActivity.grantRWPermissions(data: Intent?) {
    try {
        val uri = data?.data
        grantUriPermission(
            packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val takeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri!!, takeFlags)
    } catch (e: Exception) {
        Log.e("Main", "SD card permission error.", e)
    }
}
