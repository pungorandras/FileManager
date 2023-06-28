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

suspend fun FileManagerActivity.asyncCopySelected(dstPath: File) {
    progressBar = progressBarBuilder(R.string.copying)
    selectedListSize = 0.0
    progressState = 0.0

    job = CoroutineScope(IO).launch {
        val selectedList = fmAdapter.getSelectedList().toList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val dstObject = File(dstPath.path + "/" + element.name)

            if (!dstPath.path.contains(element.path)) {
                if (!dstObject.exists())
                    copy(element, dstPath)
                else
                    withContext(Main) { alreadyExistsDialog(element.name) }
            } else
                withContext(Main) { copyOrMoveIntoItselfDialog("copy") }

            if (!isActive)
                return@launch
        }
    }

    job.join()
    setProgressLayoutVisibility(View.GONE)
    listFiles()
    resetProgressBar()
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
            return
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
            dstFile?.parentFile?.let { copyToSDCard(src, it) }

        if (!job.isActive)
            return
    }
}

fun FileManagerActivity.copy(srcObj: AboutFile, dstPath: File) {
    val dstFile = File(dstPath.path + "/" + srcObj.name)
    val srcObject = File(srcObj.path)

    if (srcObj.mimeType == TYPE_FOLDER) {
        if (dstPath.path.contains(rootPath.path) || vcIsR)
            copyFolderToInternal(srcObject, dstPath)
        else
            copyFolderToSDCard(srcObject, dstPath)
    } else {
        progressState += File(srcObj.path).length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (dstPath.path.contains(rootPath.path) || vcIsR)
            srcObject.copyTo(dstFile)
        else
            copyToSDCard(srcObject, dstPath)
    }
}