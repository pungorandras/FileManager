package hu.pungor.filemanager.permissions

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import java.io.File
import java.util.regex.Pattern

class SDCardPermissionsUntilApi29 {

    private fun sdCardPermissions(activity: FileManagerActivity) {
        sdCardPermissionsBuilder(activity)
    }

    fun getSDCardPath(activity: FileManagerActivity): File? {
        val pattern = Pattern.compile("([A-Z]|[0-9]){4}-([A-Z]|[0-9]){4}")
        val regex = pattern.toRegex()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (element in activity.applicationContext.externalMediaDirs) {
                val path = regex.find(element.path)
                if (path != null) {
                    return File("/storage/" + path.value)
                }
            }
        } else if ((getUri(activity) == null)) {
            sdCardPermissionsBuilder(activity)
            return File("/storage/" + getUri(activity)?.path?.let { regex.find(it)?.value })
        }
        return null
    }

    fun getUri(activity: FileManagerActivity): Uri? {
        try {
            val persistedUriPermissions =
                activity.contentResolver.persistedUriPermissions
            if (persistedUriPermissions.size > 0) {
                val uriPermission = persistedUriPermissions[0]
                return uriPermission.uri
            }
        } catch (e: Exception) {
            sdCardPermissions(activity)
        }
        return null
    }

    @SuppressLint("InflateParams")
    private fun sdCardPermissionsBuilder(activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.info
        )
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text =
            activity.getString(R.string.sdcard_permission)

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->
                activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
            }
        builder.show()
    }

    fun activityResult(requestCode: Int, data: Intent?, activity: FileManagerActivity) {
        try {
            if (requestCode == 1001) {
                val uri = data?.data
                activity.grantUriPermission(
                    activity.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val takeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                activity.contentResolver.takePersistableUriPermission(uri!!, takeFlags)
            }
        } catch (e: Exception) {
        }
    }
}