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
import ir.nardana.linearprogressbar.LinearProgressBar
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File

private var selectedListSize = 0.0
private var copyState = 0.0
private lateinit var copyJob: Job
private lateinit var progressBar: LinearProgressBar

@Suppress("DEPRECATION")
suspend fun FileManagerActivity.asyncCopySelected() {
    selectedListSize = 0.0
    copyState = 0.0
    progressBar = progressBarBuilder(R.string.copying)

    copyJob = CoroutineScope(IO).launch {
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

    copyJob.join()
    setProgressLayoutVisibility(View.GONE)
    listFiles()
    setProgressBarState(progressBar, 0.0)
}

@Suppress("DEPRECATION")
private fun copyFolderToInternal(srcPath: File, dstPath: File) {
    for (src in srcPath.walkTopDown()) {
        val relPath = srcPath.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(dstPath, it) }

        copyState += src.length()
        setProgressBarState(progressBar, copyState * 100 / selectedListSize)

        if (src.isDirectory)
            dstFile?.mkdirs()
        else
            dstFile?.let { src.copyTo(it) }

        if (!copyJob.isActive)
            break
    }
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copyFolderToSDCard(srcPath: File, dstPath: File) {
    for (src in srcPath.walkTopDown()) {
        val relPath = srcPath.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(dstPath, it) }

        copyState += src.length()
        setProgressBarState(progressBar, copyState * 100 / selectedListSize)

        if (src.isDirectory)
            dstFile?.parentFile?.let { createFolderOnSDCard(it, src.name) }
        else
            dstFile?.parentFile?.let { copyToSDCard(it, src) }

        if (!copyJob.isActive)
            break
    }
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.copy(fileObject: AboutFile, dstPath: File) {
    val dstFile = File(dstPath.path + "/" + fileObject.name)
    val srcObject = File(fileObject.path)

    if (fileObject.mimeType == TYPE_FOLDER) {
        if (dstPath.path.contains(rootPath.path) || vcIsR)
            copyFolderToInternal(srcObject, dstPath)
        else
            copyFolderToSDCard(srcObject, dstPath)
    } else {
        copyState += File(fileObject.path).length()
        setProgressBarState(progressBar, copyState * 100 / selectedListSize)

        if (dstPath.path.contains(rootPath.path) || vcIsR)
            srcObject.copyTo(dstFile)
        else
            copyToSDCard(srcObject, dstFile)
    }
}