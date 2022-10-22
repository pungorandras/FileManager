package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.deleteOnSDCard
import hu.pungor.filemanager.operations.getChildren
import kotlinx.coroutines.*
import java.io.File

private var selectedListSize = 0.0
private var deleteState = 0.0
lateinit var deleteJob: Job
private lateinit var progressDialog: ProgressDialog

@Suppress("DEPRECATION")
suspend fun FileManagerActivity.asyncDeleteSelected() {
    selectedListSize = 0.0
    deleteState = 0.0

    progressDialog = progressDialogBuilder(
        titleText = R.string.deleting,
        buttonFunctionality = { deleteJob.cancel() }
    ).apply { show() }

    deleteJob = CoroutineScope(Dispatchers.IO).launch {
        val selectedList = fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (position in selectedList.indices) {
            val fileObject = selectedList[position]
            val index = (position + 1).toString()
            progressDialog.setProgressNumberFormat(index + "/" + selectedList.size)

            delete(fileObject)

            if (!isActive)
                break
        }
    }

    deleteJob.join()
    progressDialog.dismiss()
    listFiles()
}

@Suppress("DEPRECATION")
fun FileManagerActivity.deleteFolder(fileObject: File) {
    if (fileObject.isDirectory) {
        for (element in fileObject.listFiles()!!) {
            deleteState += element.length()
            progressDialog.progress = (deleteState * 100 / selectedListSize).toInt()
            deleteFolder(element)

            if (!deleteJob.isActive)
                return
        }
    }

    if (!deleteJob.isActive)
        return

    fileObject.delete()
}

@Suppress("DEPRECATION")
fun FileManagerActivity.deleteFolderOnSDCard(fileObject: DocumentFile) {
    if (fileObject.isDirectory) {
        for (element in fileObject.listFiles()) {
            deleteState += element.length()
            progressDialog.progress = (deleteState * 100 / selectedListSize).toInt()
            deleteFolderOnSDCard(element)

            if (!deleteJob.isActive)
                return
        }
    }

    if (!deleteJob.isActive)
        return

    fileObject.delete()
}

@Suppress("DEPRECATION")
fun FileManagerActivity.delete(fileObject: AboutFile) {
    val file = File(fileObject.path)

    if (fileObject.mimeType == TYPE_FOLDER)
        if (currentPath.path.contains(rootPath.path) || vcIsR)
            deleteFolder(file)
        else {
            val sdCardFolder = getChildren(currentPath)?.findFile(file.name)
            if (sdCardFolder != null)
                deleteFolderOnSDCard(sdCardFolder)
        }
    else {
        deleteState += File(fileObject.path).length()
        progressDialog.progress = (deleteState * 100 / selectedListSize).toInt()

        if (currentPath.path.contains(rootPath.path) || vcIsR)
            file.delete()
        else
            deleteOnSDCard(currentPath, file)
    }
}

