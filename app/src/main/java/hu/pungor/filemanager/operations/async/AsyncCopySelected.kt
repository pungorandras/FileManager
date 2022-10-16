package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.copyOrMoveIntoItselfDialog
import hu.pungor.filemanager.operations.copyToSDCard
import hu.pungor.filemanager.operations.createFolderOnSDCard
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File

private var selectedListSize = 0.0
private var copyState = 0.0
lateinit var progressDialog: ProgressDialog

@Suppress("DEPRECATION")
suspend fun FileManagerActivity.asyncCopySelected() {

    progressDialog = withContext(Main) {
        progressDialogBuilder(
            titleText = R.string.copying,
            buttonFunctionality = { cancel() }
        ).apply { show() }
    }

    CoroutineScope(IO).launch {
        val selectedList = fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val file = File(currentPath.path + "/" + element.name)
            val index = (selectedList.indexOf(element) + 1).toString()
            progressDialog.setProgressNumberFormat((index + "/" + selectedList.size))

            if (!file.exists()) {
                if (element.mimeType == TYPE_FOLDER && !currentPath.path.contains(element.path)) {
                    if (currentPath.path.contains(rootPath.path) || vcIsR)
                        copyFolder(File(element.path))
                    else
                        copyFolderToSDCard(File(element.path))
                } else if (!currentPath.path.contains(element.path)) {
                    copyState += File(element.path).length()
                    progressDialog.progress = (copyState * 100 / selectedListSize).toInt()

                    if (currentPath.path.contains(rootPath.path) || vcIsR)
                        File(element.path).copyTo(file)
                    else
                        copyToSDCard(currentPath, File(element.path))
                }
            } else if (file.exists()) {
                withContext(Main) {
                    alreadyExistsDialog(element.name)
                }
            } else {
                withContext(Main) {
                    copyOrMoveIntoItselfDialog("copy")
                }
            }

            if (!isActive)
                break
        }

        TODO("cancel job + dismiss dialog + list files")
//        progressDialog.dismiss()
//        listFiles()
    }
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copyFolder(folder: File) {
    CoroutineScope(IO).launch {
        for (src in folder.walkTopDown()) {
            val relPath = folder.parentFile?.let { src.toRelativeString(it) }
            val dstFile = relPath?.let { File(currentPath, it) }

            copyState += src.length()
            progressDialog.progress = (copyState * 100 / selectedListSize).toInt()

            if (src.isDirectory)
                dstFile?.mkdirs()
            else
                dstFile?.let { src.copyTo(it) }

            if (!isActive)
                break
        }
    }

}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copyFolderToSDCard(folder: File) {
    CoroutineScope(IO).launch {
        for (src in folder.walkTopDown()) {
            val relPath = folder.parentFile?.let { src.toRelativeString(it) }
            val dstFile = relPath?.let { File(currentPath, it) }

            copyState += src.length()
            progressDialog.progress = (copyState * 100 / selectedListSize).toInt()

            if (src.isDirectory)
                dstFile?.parentFile?.let { createFolderOnSDCard(it, src.name) }
            else
                dstFile?.parentFile?.let { copyToSDCard(it, src) }

            if (!isActive)
                break
        }
    }
}