package hu.pungor.filemanager.operations

import android.webkit.MimeTypeMap
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.model.AboutFile
import java.io.File
import java.util.*

fun FileManagerActivity.fillList(fileList: List<File>): List<AboutFile> {
    val mutableFileList = mutableListOf<AboutFile>()

    if (fileList.isNotEmpty()) {
        for (currentFile in fileList) {
            val uri = currentFile.path
            val uriWithoutPrefix = uri.removePrefix("/storage/emulated/0/")
            val extension = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            val name = if (fmAdapter.btnSearchPressed) uriWithoutPrefix else currentFile.name

            if (currentFile.isDirectory)
                mutableFileList += AboutFile(name, "", uri, FileManagerActivity.TYPE_FOLDER, false)
            else if (mimeType.isNullOrEmpty())
                mutableFileList += AboutFile(
                    name,
                    getSize(currentFile),
                    uri,
                    FileManagerActivity.TYPE_UNKNOWN,
                    false
                )
            else
                mutableFileList += AboutFile(
                    name,
                    getSize(currentFile),
                    uri,
                    mimeType.toString(),
                    false
                )
        }
    }
    return sortList(mutableFileList)
}

private fun getSize(file: File): String {
    val GB: Long = 1024 * 1024 * 1024
    val MB: Long = 1024 * 1024
    val kB: Long = 1024
    val size_in_bytes = file.length().toDouble()

    if (size_in_bytes > GB)
        return String.format("%.1f", size_in_bytes / GB) + "\u00A0GB"
    else if (size_in_bytes > MB)
        return String.format("%.1f", size_in_bytes / MB) + "\u00A0MB"
    else if (size_in_bytes > kB)
        return String.format("%.1f", size_in_bytes / kB) + "\u00A0kB"
    else
        return String.format("%.1f", size_in_bytes) + "\u00A0B"
}

private fun sortList(list: List<AboutFile>): List<AboutFile> {
    val folders = list.filter { it.mimeType == FileManagerActivity.TYPE_FOLDER }
    val files = list.subtract(folders.toSet())
    return folders.sortedWith(compareBy { it.name }) + files.sortedWith(compareBy { it.name })
}