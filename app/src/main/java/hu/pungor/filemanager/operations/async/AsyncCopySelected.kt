package hu.pungor.filemanager.operations.async

import android.view.View
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.copyOrMoveIntoItselfDialog
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.copyToSDCard
import hu.pungor.filemanager.operations.createFolderOnSDCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

suspend fun FileManagerActivity.asyncCopySelected() {
    selectedListSize = 0.0
    progressState = 0.0
    progressBar = progressBarBuilder(R.string.copying)

    job = CoroutineScope(IO).launch {
        val selectedList = fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val dstObject = File(currentPath.path + "/" + element.name)

            if (!currentPath.path.contains(element.path)) {
                if (!dstObject.exists())
                    copy(element, currentPath)
                else
                    withContext(Main) { alreadyExistsDialog(element.name) }
            } else
                withContext(Main) { copyOrMoveIntoItselfDialog("copy") }

            if (!isActive)
                break
        }
    }

    job.join()
    setProgressLayoutVisibility(View.GONE)
    listFiles()
}

private fun FileManagerActivity.copyFolderToInternal(srcPath: File, dstPath: File) {
    for (src in srcPath.walkTopDown()) {
        val relPath = srcPath.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(dstPath, it) }

        progressState += src.length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (src.isDirectory)
            dstFile?.mkdirs()
        else
            dstFile?.let { src.copyTo(it) }

        if (!job.isActive)
            break
    }
}

private fun FileManagerActivity.copyFolderToSDCard(srcPath: File, dstPath: File) {
    for (src in srcPath.walkTopDown()) {
        val relPath = srcPath.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(dstPath, it) }

        progressState += src.length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (src.isDirectory)
            dstFile?.parentFile?.let { createFolderOnSDCard(it, src.name) }
        else
            dstFile?.parentFile?.let { copyToSDCard(it, src) }

        if (!job.isActive)
            break
    }
}

private fun FileManagerActivity.copy(fileObject: AboutFile, dstPath: File) {
    val dstFile = File(dstPath.path + "/" + fileObject.name)
    val srcObject = File(fileObject.path)

    if (fileObject.mimeType == TYPE_FOLDER) {
        if (dstPath.path.contains(rootPath.path) || vcIsR)
            copyFolderToInternal(srcObject, dstPath)
        else
            copyFolderToSDCard(srcObject, dstPath)
    } else {
        progressState += File(fileObject.path).length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (dstPath.path.contains(rootPath.path) || vcIsR)
            srcObject.copyTo(dstFile)
        else
            copyToSDCard(srcObject, dstFile)
    }
}