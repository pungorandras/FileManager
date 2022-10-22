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
import hu.pungor.filemanager.operations.deleteOnSDCard
import hu.pungor.filemanager.operations.getChildren
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.io.File

private var selectedListSize = 0.0
private var moveState = 0.0
private lateinit var moveJob: Job
private lateinit var progressDialog: ProgressDialog

@Suppress("DEPRECATION")
suspend fun FileManagerActivity.asyncMoveSelected() {
    selectedListSize = 0.0
    moveState = 0.0

    progressDialog = progressDialogBuilder(
        titleText = R.string.moving,
        buttonFunctionality = { moveJob.cancel() }
    ).apply { show() }

    moveJob = CoroutineScope(Dispatchers.IO).launch {
        val selectedList = fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val dstObj = File(currentPath.path + "/" + element.name)
            val index = (selectedList.indexOf(element) + 1).toString()
            progressDialog.setProgressNumberFormat(index + "/" + selectedList.size)

            if (!currentPath.path.contains(element.path)) {
                if (!dstObj.exists())
                    move(element, dstObj)
                else
                    withContext(Main) { alreadyExistsDialog(dstObj.name) }
            } else
                withContext(Main) { copyOrMoveIntoItselfDialog("move") }

            if (!isActive)
                break
        }
    }

    moveJob.join()
    progressDialog.dismiss()
    listFiles()
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.moveFolderToInternal(folder: File) {
    for (src in folder.walkTopDown()) {
        val relPath = folder.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(currentPath, it) }

        moveState += src.length()
        progressDialog.progress = (moveState * 100 / selectedListSize).toInt()

        if (src.isDirectory)
            dstFile?.mkdirs()
        else
            dstFile?.let { src.copyTo(it) }

        if (!moveJob.isActive) {
            deleteJob.cancel()
            break
        }
    }
}

@Suppress("DEPRECATION")
private fun FileManagerActivity.moveFolderToSDCard(folder: File) {
    for (src in folder.walkTopDown()) {
        val relPath = folder.parentFile?.let { src.toRelativeString(it) }
        val dstFile = relPath?.let { File(currentPath, it) }

        moveState += src.length()
        progressDialog.progress = (moveState * 100 / selectedListSize).toInt()

        if (src.isDirectory)
            dstFile?.parentFile?.let { createFolderOnSDCard(it, src.name) }
        else
            dstFile?.parentFile?.let { copyToSDCard(it, src) }

        if (!moveJob.isActive) {
            deleteJob.cancel()
            break
        }
    }
}

private fun FileManagerActivity.folderMovingLogicToSDCard(folder: AboutFile) {
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

@Suppress("DEPRECATION")
private fun FileManagerActivity.fileMovingLogicToSDCard(file: AboutFile, dstFile: File) {
    val fileObj = File(file.path)

    moveState += fileObj.length()
    progressDialog.progress = (moveState * 100 / selectedListSize).toInt()

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

private fun FileManagerActivity.folderMovingLogicToInternal(
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

@Suppress("DEPRECATION")
private fun FileManagerActivity.fileMovingLogicToInternal(file: AboutFile, dstFile: File) {
    val fileObj = File(file.path)

    moveState += fileObj.length()
    progressDialog.progress = (moveState * 100 / selectedListSize).toInt()

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
            folderMovingLogicToSDCard(srcObj)
        else
            fileMovingLogicToSDCard(srcObj, dstObj)
    } else if (currentPath.path.contains(rootPath.path)) {
        if (srcObj.mimeType == TYPE_FOLDER)
            folderMovingLogicToInternal(srcObj, dstObj)
        else
            fileMovingLogicToInternal(srcObj, dstObj)
    }
}