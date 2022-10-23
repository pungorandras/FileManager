package hu.pungor.filemanager.operations

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.core.content.FileProvider
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alertDialogBuilder
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.nameIsNullDialog
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.async.*
import hu.pungor.filemanager.permissions.checkPermissionsAndLoadFiles
import java.io.File
import java.io.FileWriter

fun FileManagerActivity.openFile(file: AboutFile) {
    val intent = Intent(Intent.ACTION_VIEW)

    if (currentPath.path.contains(rootPath.path) || vcIsR) {
        intent.setDataAndType(
            FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                File(file.path)
            ), file.mimeType
        )
    } else {
        val uri = getChildren(currentPath)?.findFile(file.name)?.uri
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
    if (file.mimeType == TYPE_FOLDER) {
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
            this,
            "$packageName.provider",
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
        createTextFileOnSDCard(name, notes)
        listFiles()
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
        createFolderOnSDCard(currentPath, name)
        listFiles()
    }
}

fun FileManagerActivity.shareFile(view: View, position: Int) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(
        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
            view.context, "$packageName.provider",
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

    alertDialogBuilder(
        titleText = R.string.rename,
        dialogLayout = dialogView,
        positiveButtonFunctionality = {
            val file = File(currentItem.path)
            val newName = dialogView.findViewById<EditText>(R.id.name_input).text.toString()

            if (newName.isNotEmpty()) {
                if (sdCardPath?.let { currentPath.path.contains(it.path) } == true || vcIsR)
                    file.renameTo(File(currentPath, newName))
                else
                    renameOnSDCard(currentItem, newName)

                listFiles()
            } else
                nameIsNullDialog()
        },
        negativeButtonLabel = R.string.cancel
    ).show()
}

suspend fun FileManagerActivity.deleteSelectedFiles() {
    asyncDeleteSelected()
}

suspend fun FileManagerActivity.copySelectedFiles() {
    asyncCopySelected()
}

suspend fun FileManagerActivity.moveSelectedFiles() {
    asyncMoveSelected()
}

suspend fun FileManagerActivity.search(input: String): MutableList<File> {
    return asyncSearch(input)
}
