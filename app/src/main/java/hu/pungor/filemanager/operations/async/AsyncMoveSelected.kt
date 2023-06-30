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
import hu.pungor.filemanager.operations.deleteOnSDCard
import hu.pungor.filemanager.operations.getChildren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

suspend fun FileManagerActivity.asyncMoveSelected(dstPath: File) {
    progressBar = progressBarBuilder(R.string.moving)
    selectedListSize = 0.0
    progressState = 0.0

    job = CoroutineScope(Dispatchers.IO).launch {
        val selectedList = fmAdapter.getSelectedList().toList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val dstObj = File(dstPath.path + "/" + element.name)

            if (!dstPath.path.contains(element.path)) {
                if (!dstObj.exists())
                    move(element, dstObj)
                else
                    withContext(Main) { alreadyExistsDialog(dstObj.name) }
            } else
                withContext(Main) { copyOrMoveIntoItselfDialog("move") }

            if (!isActive)
                return@launch
        }
    }

    job.join()
    setProgressLayoutVisibility(View.GONE)
    listFiles()
    resetProgressBar()
}

private fun FileManagerActivity.moveFolderToInternal(folder: File) {
    for (src in folder.walkTopDown()) {
        val relPath = folder.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(currentPath, it) }

        progressState += src.length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (src.isDirectory)
            dstFile?.mkdirs()
        else
            dstFile?.let { src.copyTo(it) }

        if (!job.isActive) {
            return
        }
    }
}

private fun FileManagerActivity.moveFolderToSDCard(folder: File) {
    for (src in folder.walkTopDown()) {
        val relPath = folder.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(currentPath, it) }

        progressState += src.length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (src.isDirectory)
            dstFile?.parentFile?.let { createFolderOnSDCard(it, src.name) }
        else
            dstFile?.parentFile?.let { copyToSDCard(src, it) }

        if (!job.isActive) {
            return
        }
    }
}

private fun FileManagerActivity.folderMovingStepsToSDCard(folder: AboutFile) {
    val folderObj = File(folder.path)

    if (vcIsR)
        moveFolderToInternal(folderObj)
    else
        moveFolderToSDCard(folderObj)

    if (folder.path.contains(rootPath.path) || vcIsR)
        deleteFolder(folderObj)
    else {
        val sdCardFolder = getChildren(folderObj)
        if (sdCardFolder != null)
            deleteFolderOnSDCard(sdCardFolder)
    }
}

private fun FileManagerActivity.fileMovingStepsToSDCard(file: AboutFile, dstFile: File) {
    val fileObj = File(file.path)

    progressState += fileObj.length()
    setProgressBarState(progressBar, progressState * 100 / selectedListSize)

    if (vcIsR)
        fileObj.copyTo(dstFile)
    else
        copyToSDCard(currentPath, fileObj)

    if (file.path.contains(rootPath.path) || vcIsR)
        fileObj.delete()
    else {
        val path = fileObj.parentFile?.path?.let { File(it) }
        if (path != null)
            deleteOnSDCard(path, fileObj)
    }
}

private fun FileManagerActivity.folderMovingStepsToInternal(
    folder: AboutFile,
    dstFolder: File
) {
    val folderObj = File(folder.path)

    if (folder.path.contains(rootPath.path))
        File(folder.path).renameTo(dstFolder)
    else {
        moveFolderToInternal(folderObj)
        if (vcIsR)
            deleteFolder(folderObj)
        else {
            val sdCardFolder = getChildren(folderObj)
            if (sdCardFolder != null)
                deleteFolderOnSDCard(sdCardFolder)
        }
    }
}

private fun FileManagerActivity.fileMovingStepsToInternal(file: AboutFile, dstFile: File) {
    val fileObj = File(file.path)

    progressState += fileObj.length()
    setProgressBarState(progressBar, progressState * 100 / selectedListSize)

    if (file.path.contains(rootPath.path))
        fileObj.renameTo(dstFile)
    else {
        fileObj.copyTo(dstFile)
        if (vcIsR)
            fileObj.delete()
        else {
            val path = fileObj.parentFile?.path?.let { File(it) }
            if (path != null)
                deleteOnSDCard(path, fileObj)
        }
    }
}

private fun FileManagerActivity.move(srcObj: AboutFile, dstObj: File) {
    if (sdCardPath?.let { currentPath.path.contains(it.path) } == true) {
        if (srcObj.mimeType == TYPE_FOLDER)
            folderMovingStepsToSDCard(srcObj)
        else
            fileMovingStepsToSDCard(srcObj, dstObj)
    } else if (currentPath.path.contains(rootPath.path)) {
        if (srcObj.mimeType == TYPE_FOLDER)
            folderMovingStepsToInternal(srcObj, dstObj)
        else
            fileMovingStepsToInternal(srcObj, dstObj)
    }
}
