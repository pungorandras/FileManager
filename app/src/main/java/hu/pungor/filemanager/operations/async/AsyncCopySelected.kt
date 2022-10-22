package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.copyOrMoveIntoItselfDialog
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.copyToSDCard
import hu.pungor.filemanager.operations.createFolderOnSDCard
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File

private var selectedListSize = 0.0
private var copyState = 0.0
private lateinit var copyJob: Job
private lateinit var progressDialog: ProgressDialog

@Suppress("DEPRECATION")
suspend fun FileManagerActivity.asyncCopySelected() {
    selectedListSize = 0.0
    copyState = 0.0

    progressDialog = progressDialogBuilder(
        titleText = R.string.copying,
        buttonFunctionality = { copyJob.cancel() }
    ).apply { show() }

    copyJob = CoroutineScope(IO).launch {
        val selectedList = fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val dstObj = File(currentPath.path + "/" + element.name)
            val index = (selectedList.indexOf(element) + 1).toString()
            progressDialog.setProgressNumberFormat(index + "/" + selectedList.size)

            if (!currentPath.path.contains(element.path)) {
                if (!dstObj.exists())
                    copy(element)
                else
                    withContext(Main) { alreadyExistsDialog(element.name) }
            } else
                withContext(Main) { copyOrMoveIntoItselfDialog("copy") }

            if (!isActive)
                break
        }
    }

    copyJob.join()
    progressDialog.dismiss()
    listFiles()
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copyFolderToInternal(folder: File) {
    for (src in folder.walkTopDown()) {
        val relPath = folder.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(currentPath, it) }

        copyState += src.length()
        progressDialog.progress = (copyState * 100 / selectedListSize).toInt()

        if (src.isDirectory)
            dstFile?.mkdirs()
        else
            dstFile?.let { src.copyTo(it) }

        if (!copyJob.isActive)
            break
    }
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copyFolderToSDCard(folder: File) {
    for (src in folder.walkTopDown()) {
        val relPath = folder.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(currentPath, it) }

        copyState += src.length()
        progressDialog.progress = (copyState * 100 / selectedListSize).toInt()

        if (src.isDirectory)
            dstFile?.parentFile?.let { createFolderOnSDCard(it, src.name) }
        else
            dstFile?.parentFile?.let { copyToSDCard(it, src) }

        if (!copyJob.isActive)
            break
    }
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copy(fileObject: AboutFile) {
    val file = File(currentPath.path + "/" + fileObject.name)

    if (fileObject.mimeType == TYPE_FOLDER) {
        if (currentPath.path.contains(rootPath.path) || vcIsR)
            copyFolderToInternal(File(fileObject.path))
        else
            copyFolderToSDCard(File(fileObject.path))
    } else {
        copyState += File(fileObject.path).length()
        progressDialog.progress = (copyState * 100 / selectedListSize).toInt()

        if (currentPath.path.contains(rootPath.path) || vcIsR)
            File(fileObject.path).copyTo(file)
        else
            copyToSDCard(currentPath, File(fileObject.path))
    }
}