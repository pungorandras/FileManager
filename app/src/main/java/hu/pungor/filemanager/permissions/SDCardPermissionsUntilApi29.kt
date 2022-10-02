package hu.pungor.filemanager.permissions

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import java.io.File
import java.util.regex.Pattern

fun FileManagerActivity.getSDCardPath(): File? {
    val pattern = Pattern.compile("([A-Z]|[0-9]){4}-([A-Z]|[0-9]){4}")
    val regex = pattern.toRegex()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        for (element in externalMediaDirs) {
            val path = regex.find(element.path)
            if (path != null) {
                return File("/storage/" + path.value)
            }
        }
    } else if ((getUri() == null)) {
        sdCardPermissionsBuilder()
        return File("/storage/" + getUri()?.path?.let { regex.find(it)?.value })
    }
    return null
}

fun FileManagerActivity.getUri(): Uri? {
    try {
        val persistedUriPermissions =
            contentResolver.persistedUriPermissions
        if (persistedUriPermissions.size > 0) {
            val uriPermission = persistedUriPermissions[0]
            return uriPermission.uri
        }
    } catch (e: Exception) {
        sdCardPermissionsBuilder()
    }
    return null
}

@SuppressLint("InflateParams")
fun FileManagerActivity.sdCardPermissionsBuilder() {
    val customTitle =
        LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = getString(R.string.info)
    val customText =
        LayoutInflater.from(this).inflate(R.layout.custom_text_alertdialog, null)
    customText.findViewById<TextView>(R.id.custom_text).text = getString(R.string.sdcard_permission)

    val builder = AlertDialog.Builder(this)
        .setCustomTitle(customTitle)
        .setView(customText)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
        }
    builder.show()
}

fun FileManagerActivity.activityResult(requestCode: Int, data: Intent?) {
    try {
        if (requestCode == 1001) {
            val uri = data?.data
            grantUriPermission(
                packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val takeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri!!, takeFlags)
        }
    } catch (e: Exception) {
        Log.e("Main", "SD card permission error.", e)
    }
}
