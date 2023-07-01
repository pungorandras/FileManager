package hu.pungor.filemanager.operations.async

import android.view.View
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.copyOrMoveIntoItselfDialog
import hu.pungor.filemanager.model.AboutFile
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
                    move(element, dstPath)
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

private fun FileManagerActivity.move(srcObj: AboutFile, dstPath: File) {
    if (srcObj.path.contains(rootPath.path) && dstPath.path.contains(rootPath.path)) {
        setProgressBarState(progressBar, 100.0)
        File(srcObj.path).renameTo(File(dstPath.path + "/" + srcObj.name))
    } else {
        copy(srcObj, dstPath, updateProgress = srcObj.mimeType == TYPE_FOLDER)
        File(srcObj.path).parentFile?.let { delete(srcObj, it) }
    }
}
