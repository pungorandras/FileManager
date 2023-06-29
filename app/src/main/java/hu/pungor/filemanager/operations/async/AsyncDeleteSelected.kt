package hu.pungor.filemanager.operations.async

import android.view.View
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.deleteOnSDCard
import hu.pungor.filemanager.operations.getChildren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

suspend fun FileManagerActivity.asyncDeleteSelected(filePath: File) {
    progressBar = progressBarBuilder(R.string.deleting)
    selectedListSize = 0.0
    progressState = 0.0

    job = CoroutineScope(Dispatchers.IO).launch {
        val selectedList = fmAdapter.getSelectedList().toList()
        selectedListSize = getSelectedListSize(selectedList)

        for (position in selectedList.indices) {
            val fileObject = selectedList[position]
            delete(fileObject, filePath)

            if (!isActive)
                return@launch
        }
    }

    job.join()
    setProgressLayoutVisibility(View.GONE)
    listFiles()
    resetProgressBar()
}

fun FileManagerActivity.deleteFolder(fileObject: File) {
    if (fileObject.isDirectory) {
        for (element in fileObject.listFiles()!!) {
            progressState += element.length()
            setProgressBarState(progressBar, progressState * 100 / selectedListSize)
            deleteFolder(element)

            if (!job.isActive)
                return
        }
    }

    if (!job.isActive)
        return

    fileObject.delete()
}

fun FileManagerActivity.deleteFolderOnSDCard(fileObject: DocumentFile) {
    if (fileObject.isDirectory) {
        for (element in fileObject.listFiles()) {
            progressState += element.length()
            setProgressBarState(progressBar, progressState * 100 / selectedListSize)
            deleteFolderOnSDCard(element)

            if (!job.isActive)
                return
        }
    }

    if (!job.isActive)
        return

    fileObject.delete()
}

fun FileManagerActivity.delete(fileObject: AboutFile, filePath: File) {
    val file = File(fileObject.path)

    if (fileObject.mimeType == TYPE_FOLDER)
        if (filePath.path.contains(rootPath.path) || vcIsR)
            deleteFolder(file)
        else {
            val sdCardFolder = getChildren(filePath)?.findFile(file.name)
            if (sdCardFolder != null)
                deleteFolderOnSDCard(sdCardFolder)
        }
    else {
        progressState += file.length()
        setProgressBarState(progressBar, progressState * 100 / selectedListSize)

        if (filePath.path.contains(rootPath.path) || vcIsR)
            file.delete()
        else
            deleteOnSDCard(filePath, file)
    }
}

