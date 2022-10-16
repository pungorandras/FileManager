package hu.pungor.filemanager.operations.async

import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.operations.fillList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

suspend fun FileManagerActivity.asyncGetAllFiles() {
    val fileList = CoroutineScope(IO).async {
        return@async currentPath.listFiles()?.asList()?.let { fillList(it) }
    }
    fileList.await()?.let { fmAdapter.setFiles(it) }
}

fun FileManagerActivity.listFiles(fileList: List<File>? = null) {
    if (fileList == null)
        CoroutineScope(Main).launch { asyncGetAllFiles() }
    else
        CoroutineScope(Main).launch { fmAdapter.setFiles(fillList(fileList)) }
}

fun FileManagerActivity.listFilesRunBlocking() {
    runBlocking { asyncGetAllFiles() }
}