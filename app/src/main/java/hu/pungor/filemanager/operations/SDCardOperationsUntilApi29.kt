package hu.pungor.filemanager.operations

import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.permissions.getUri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream

fun FileManagerActivity.copyToSDCard(file: File, dstPath: File) {
    val sdCardFile = getChildren(dstPath)?.createFile("", file.name)
    val bufferSize = 8 * 1024
    val bufferedInput = BufferedInputStream(FileInputStream(file))
    val bufferedOutput =
        sdCardFile?.uri?.let { BufferedOutputStream(contentResolver.openOutputStream(it)) }

    bufferedInput.use { input ->
        bufferedOutput.use { output ->
            if (output != null)
                input.copyTo(output, bufferSize)
        }
    }

    bufferedInput.close()
    bufferedOutput?.flush()
    bufferedOutput?.close()
}

fun FileManagerActivity.createTextFileOnSDCard(name: String, notes: String) {
    val sdCardFile = getChildren(currentPath)?.createFile("", "$name.txt")
    val output = sdCardFile?.uri?.let { contentResolver.openOutputStream(it) }
    output?.write(notes.toByteArray())
    output?.flush()
    output?.close()
}

fun FileManagerActivity.createFolderOnSDCard(dstPath: File, name: String) {
    getChildren(dstPath)?.createDirectory(name)
}

fun FileManagerActivity.renameOnSDCard(input: AboutFile, newName: String) {
    getChildren(currentPath)?.findFile(input.name)?.renameTo(newName)
}

fun FileManagerActivity.deleteOnSDCard(path: File, file: File) {
    getChildren(path)?.findFile(file.name)?.delete()
}

fun FileManagerActivity.getChildren(dstPath: File): DocumentFile? {
    try {
        var id = DocumentsContract.getTreeDocumentId(getUri())
        id += sdCardPath?.let { dstPath.path.removePrefix(it.path) }
        val childrenUri = DocumentsContract.buildDocumentUriUsingTree(getUri(), id)
        return DocumentFile.fromTreeUri(this, childrenUri)
    } catch (e: Exception) {
        Log.e("Main", "Unable to get child object.", e)
    }
    return null
}
