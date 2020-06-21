package hu.pungor.filemanager.operations

import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.model.AboutFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream

class SDCardOperations {

    fun copyToSDCard(dstPath: File, file: File, activity: FileManagerActivity) {
        val sdCardFile = getChildren(dstPath, activity)?.createFile("", file.name)
        val buffer = ByteArray(8 * 1024)
        val bufferedInput = BufferedInputStream(FileInputStream(file))
        val bufferedOutput = sdCardFile?.uri?.let { BufferedOutputStream(activity.contentResolver.openOutputStream(it)) }

        while (bufferedInput.read(buffer, 0, buffer.size) >= 0) {
            bufferedOutput?.write(buffer, 0, buffer.size)
        }
    }

    fun createTextFileOnSDCard(name: String, notes: String, activity: FileManagerActivity) {
        val sdCardFile = getChildren(activity.currentPath, activity)?.createFile("", "$name.txt")
        val output = sdCardFile?.uri?.let { activity.contentResolver.openOutputStream(it) }
        output?.write(notes.toByteArray())
        output?.flush()
        output?.close()
    }

    fun createFolderOnSDCard(dstPath: File, name: String, activity: FileManagerActivity) {
        getChildren(dstPath, activity)?.createDirectory(name)
    }

    fun renameOnSDCard(input: AboutFile, newName: String, activity: FileManagerActivity) {
        getChildren(activity.currentPath, activity)?.findFile(input.name)?.renameTo(newName)
    }

    fun deleteOnSDCard(path: File, file: File, activity: FileManagerActivity) {
        getChildren(path, activity)?.findFile(file.name)?.delete()
    }

    fun getChildren(dstPath: File, activity: FileManagerActivity): DocumentFile? {
        var id = DocumentsContract.getTreeDocumentId(activity.getUri())
        id += dstPath.toString().removePrefix(activity.sdCardPath.toString())
        val childrenUri = DocumentsContract.buildDocumentUriUsingTree(activity.getUri(), id)

        return DocumentFile.fromTreeUri(activity, childrenUri)
    }
}