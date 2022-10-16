package hu.pungor.filemanager.operations

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.nameIsNullDialog
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.async.*
import hu.pungor.filemanager.permissions.checkPermissionsAndLoadFiles
import java.io.File
import java.io.FileWriter

private val vcIsR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

fun FileManagerActivity.openFile(file: AboutFile) {
    val intent = Intent(Intent.ACTION_VIEW)

    if (currentPath.toString().contains(rootPath.toString()) || vcIsR) {
        intent.setDataAndType(
            FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                File(file.path)
            ), file.mimeType
        )
    } else {
        val uri = getChildren(currentPath)
            ?.findFile(file.name)?.uri
        intent.setDataAndType(uri, file.mimeType)
    }

    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        openUnknown(file)
    }

}

fun FileManagerActivity.openFolder(file: AboutFile) {
    if (file.mimeType == FileManagerActivity.TYPE_FOLDER) {
        if (fmAdapter.btnSearchPressed)
            fileTreeDepth++

        currentPath = File(file.path)
        listFiles()
    }
}

fun FileManagerActivity.openUnknown(file: AboutFile) {
    val intent = Intent(Intent.ACTION_VIEW)

    intent.setDataAndType(
        FileProvider.getUriForFile(
            applicationContext, "$packageName.provider",
            File(file.path)
        ), "*/*"
    )

    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    startActivity(intent)
}

fun FileManagerActivity.createTextFile(name: String, notes: String) {
    if (currentPath.toString().contains(rootPath.toString()) || vcIsR) {
        try {
            val file = File("$currentPath/$name.txt")

            if (!file.exists() && name.isNotEmpty()) {
                val textFile = File(currentPath, "$name.txt")
                val fileWriter = FileWriter(textFile)
                fileWriter.append(notes)
                fileWriter.flush()
                fileWriter.close()
                listFiles()
            } else if (name.isEmpty())
                nameIsNullDialog()
            else
                alreadyExistsDialog(name)
        } catch (e: Exception) {
            checkPermissionsAndLoadFiles()
        }
    } else {
        try {
            createTextFileOnSDCard(name, notes)
            listFiles()
        } catch (e: Exception) {
            checkPermissionsAndLoadFiles()
        }

    }
}

fun FileManagerActivity.createFolder(name: String) {
    if (currentPath.toString().contains(rootPath.toString()) || vcIsR
    ) {
        try {
            val folder = File(currentPath, name)
            if (!folder.exists()) {
                folder.mkdir()
                listFiles()
            } else if (name.isEmpty())
                nameIsNullDialog()
            else
                alreadyExistsDialog(name)
        } catch (e: Exception) {
            checkPermissionsAndLoadFiles()
        }
    } else {
        try {
            createFolderOnSDCard(currentPath, name)
            listFiles()
        } catch (e: Exception) {
            checkPermissionsAndLoadFiles()
        }
    }
}

fun FileManagerActivity.shareFile(view: View, position: Int) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(
        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
            view.context, view.context.packageName + ".provider",
            File(fmAdapter.getItem(position).path)
        )
    )
    intent.type = "*/*"
    view.context.startActivity(intent)
}

@SuppressLint("InflateParams")
fun FileManagerActivity.renameFile(view: View, position: Int) {
    val currentItem = fmAdapter.getItem(position)

    val dialogView = LayoutInflater.from(view.context).inflate(R.layout.layout_dialog, null)
    dialogView.findViewById<EditText>(R.id.name_input).setText(currentItem.name)

    val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = getString(R.string.rename)

    val builder = AlertDialog.Builder(view.context)
        .setView(dialogView)
        .setCustomTitle(customTitle)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
            val file = File(currentItem.path)
            val newName = dialogView.findViewById<EditText>(R.id.name_input).text.toString()

            if (newName.isNotEmpty()) {
                if (currentPath.toString()
                        .contains(sdCardPath.toString()) || vcIsR
                ) {
                    file.renameTo(
                        File(
                            currentPath,
                            newName
                        )
                    )
                    listFiles()
                } else {
                    renameOnSDCard(currentItem, newName)
                    listFiles()
                }
            } else
                nameIsNullDialog()
        }
        .setNegativeButton(getString(R.string.cancel), null)
    builder.show()
}

fun FileManagerActivity.deleteSelectedFiles() {
    AsyncDeleteSelected(this).execute(this)
}

fun FileManagerActivity.copySelectedFiles() {
    AsyncCopySelected(this).execute(this)
}

fun FileManagerActivity.moveSelectedFiles() {
    AsyncMoveSelected(this).execute(this)
}

suspend fun FileManagerActivity.search(input: String): MutableList<File> {
    return asyncSearch(input)
}
