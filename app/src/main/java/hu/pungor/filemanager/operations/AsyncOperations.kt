package hu.pungor.filemanager.operations

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.AsyncTask
import android.view.LayoutInflater
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.AlertDialogMessages
import hu.pungor.filemanager.model.AboutFile
import java.io.File
import java.util.*

class AsyncGetAllFiles() : AsyncTask<FileManagerActivity, Void, List<AboutFile>>() {
    override fun doInBackground(vararg params: FileManagerActivity): List<AboutFile>? {
        val fileList = params[0].currentPath.listFiles()?.asList()

        return fileList?.let { params[0].fillList(it) }
    }
}

@Suppress("DEPRECATION")
class AsyncCopySelected(private val activity: FileManagerActivity) :
    AsyncTask<FileManagerActivity, Int, FileManagerActivity>() {

    private val progressDialog = ProgressDialog(activity)
    private val alertDialogMessages =
        AlertDialogMessages()
    private val sdCardOperations = SDCardOperations()
    private var copyState = 0.0
    private var selectedListSize = 0.0

    override fun onPreExecute() {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.copying
        )

        progressDialog.setCustomTitle(customTitle)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.max = 100
        progressDialog.progress = 0
        progressDialog.setCancelable(false)
        progressDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            activity.getString(R.string.cancel)
        ) { dialog, which ->
            this.cancel(true)
        }
        progressDialog.show()
    }

    override fun doInBackground(vararg params: FileManagerActivity): FileManagerActivity? {
        val selectedList = params[0].fileManagerAdapter.getSelectedList()
        selectedList.forEach {
            if (it.mimeType == FileManagerActivity.TYPE_FOLDER)
                selectedListSize += getFolderSize(File(it.uri))
            else
                selectedListSize += File(it.uri).length()
        }

        for (element in selectedList) {
            val file = File(params[0].currentPath.toString() + "/" + element.name)
            progressDialog.setProgressNumberFormat(((selectedList.indexOf(element) + 1).toString() + "/" + selectedList.size))

            if (element.mimeType == FileManagerActivity.TYPE_FOLDER && !params[0].currentPath.toString()
                    .contains(element.uri) && !file.exists()
            ) {
                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString()))
                    copyFolderToSDCard(File(element.uri), params[0])
                else
                    copyFolder(File(element.uri), params[0])
            } else if (!file.exists() && !params[0].currentPath.toString().contains(element.uri)) {
                copyState += File(element.uri).length()
                publishProgress((copyState * 100 / selectedListSize).toInt())

                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString()))
                    sdCardOperations.copyToSDCard(
                        params[0].currentPath,
                        File(element.uri),
                        params[0]
                    )
                else
                    File(element.uri).copyTo(file)
            } else if (file.exists()) {
                params[0].runOnUiThread {
                    alertDialogMessages.alreadyExists(element.name, params[0])
                }
            } else {
                params[0].runOnUiThread {
                    alertDialogMessages.copyOrMoveIntoItself("copy", params[0])
                }
            }

            if (this.isCancelled)
                break
        }
        return params[0]
    }

    override fun onProgressUpdate(vararg values: Int?) {
        progressDialog.progress = values[0]!!
    }

    override fun onPostExecute(result: FileManagerActivity) {
        progressDialog.hide()
        result.loadFiles()
    }

    override fun onCancelled(result: FileManagerActivity) {
        progressDialog.dismiss()
        result.loadFiles()
    }

    private fun copyFolder(folder: File, activity: FileManagerActivity) {
        for (src in folder.walkTopDown()) {
            val relPath = src.toRelativeString(folder.parentFile)
            val dstFile = File(activity.currentPath, relPath)

            copyState += src.length()
            publishProgress((copyState * 100 / selectedListSize).toInt())

            if (src.isDirectory)
                dstFile.mkdirs()
            else
                src.copyTo(dstFile)

            if (this.isCancelled)
                break
        }
    }

    private fun copyFolderToSDCard(folder: File, activity: FileManagerActivity) {
        for (src in folder.walkTopDown()) {
            val relPath = src.toRelativeString(folder.parentFile)
            val dstFile = File(activity.currentPath, relPath)

            copyState += src.length()
            publishProgress((copyState * 100 / selectedListSize).toInt())

            if (src.isDirectory)
                sdCardOperations.createFolderOnSDCard(dstFile.parentFile, src.name, activity)
            else
                sdCardOperations.copyToSDCard(dstFile.parentFile, src, activity)

            if (this.isCancelled)
                break
        }
    }
}

@Suppress("DEPRECATION")
class AsyncDeleteSelected(private val activity: FileManagerActivity) :
    AsyncTask<FileManagerActivity, Int, FileManagerActivity>() {

    private val progressDialog = ProgressDialog(activity)
    private val sdCardOperations = SDCardOperations()
    private var deleteState = 0.0
    private var selectedListSize = 0.0

    override fun onPreExecute() {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.deleting
        )

        progressDialog.setCustomTitle(customTitle)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.max = 100
        progressDialog.progress = 0
        progressDialog.setCancelable(false)
        progressDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            activity.getString(R.string.cancel)
        ) { dialog, which ->
            this.cancel(true)
        }
        progressDialog.show()
    }

    override fun doInBackground(vararg params: FileManagerActivity): FileManagerActivity {
        val selectedList = params[0].fileManagerAdapter.getSelectedList().toMutableList()
        selectedList.forEach {
            if (it.mimeType == FileManagerActivity.TYPE_FOLDER)
                selectedListSize += getFolderSize(File(it.uri))
            else
                selectedListSize += File(it.uri).length()
        }

        for (position in selectedList.indices) {
            val fileUri = selectedList[position].uri
            val file = File(fileUri)
            progressDialog.setProgressNumberFormat(((position + 1).toString() + "/" + selectedList.size))

            if (selectedList[position].mimeType == FileManagerActivity.TYPE_FOLDER)
                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString())) {
                    val sdCardFolder =
                        sdCardOperations.getChildren(params[0].currentPath, params[0])
                            ?.findFile(file.name)
                    if (sdCardFolder != null) {
                        deleteFolderOnSDCard(sdCardFolder, params[0])
                    }
                } else
                    deleteFolder(file)
            else {
                deleteState += File(selectedList[position].uri).length()
                publishProgress((deleteState * 100 / selectedListSize).toInt())

                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString()))
                    sdCardOperations.deleteOnSDCard(params[0].currentPath, file, params[0])
                else
                    file.delete()
            }

            if (this.isCancelled)
                break
        }
        return params[0]
    }

    override fun onProgressUpdate(vararg values: Int?) {
        progressDialog.progress = values[0]!!
    }

    override fun onPostExecute(result: FileManagerActivity) {
        progressDialog.hide()
        result.loadFiles()
    }

    override fun onCancelled(result: FileManagerActivity?) {
        progressDialog.dismiss()
        result?.loadFiles()
    }

    fun deleteFolder(folderOrFile: File) {
        if (folderOrFile.isDirectory) {
            for (element in folderOrFile.listFiles()) {
                deleteState += element.length()
                publishProgress((deleteState * 100 / selectedListSize).toInt())
                deleteFolder(element)

                if (this.isCancelled)
                    return
            }
        }
        if (this.isCancelled)
            return

        folderOrFile.delete()
    }

    fun deleteFolderOnSDCard(folderOrFile: DocumentFile, activity: FileManagerActivity) {
        if (folderOrFile.isDirectory) {
            for (element in folderOrFile.listFiles()) {
                deleteState += element.length()
                publishProgress((deleteState * 100 / selectedListSize).toInt())
                deleteFolderOnSDCard(element, activity)

                if (this.isCancelled)
                    return
            }
        }
        if (this.isCancelled)
            return

        folderOrFile.delete()
    }
}

@Suppress("DEPRECATION")
class AsyncMoveSelected(private val activity: FileManagerActivity) :
    AsyncTask<FileManagerActivity, Int, FileManagerActivity>() {

    private val progressDialog = ProgressDialog(activity)
    private val sdCardOperations = SDCardOperations()
    private val asyncDeleteSelected = AsyncDeleteSelected(activity)
    private val alertDialogMessages =
        AlertDialogMessages()
    private var moveState = 0.0
    private var selectedListSize = 0.0

    override fun onPreExecute() {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.moving
        )

        progressDialog.setCustomTitle(customTitle)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.max = 100
        progressDialog.progress = 0
        progressDialog.setCancelable(false)
        progressDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            activity.getString(R.string.cancel)
        ) { dialog, which ->
            this.cancel(true)
        }
        progressDialog.show()
    }

    override fun doInBackground(vararg params: FileManagerActivity): FileManagerActivity {
        val selectedList = params[0].fileManagerAdapter.getSelectedList()
        selectedList.forEach {
            if (it.mimeType == FileManagerActivity.TYPE_FOLDER)
                selectedListSize += getFolderSize(File(it.uri))
            else
                selectedListSize += File(it.uri).length()
        }

        for (element in selectedList) {
            val file = File(params[0].currentPath.toString() + "/" + element.name)
            progressDialog.setProgressNumberFormat(((selectedList.indexOf(element) + 1).toString() + "/" + selectedList.size))

            if (params[0].currentPath.toString() != element.uri && !file.exists()) {
                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString())) {
                    if (element.mimeType == FileManagerActivity.TYPE_FOLDER) {
                        moveFolderToSDCard(File(element.uri), params[0])

                        if (element.uri.contains(params[0].rootPath.toString()))
                            asyncDeleteSelected.deleteFolder(File(element.uri))
                        else {
                            val sdCardFolder =
                                sdCardOperations.getChildren(File(element.uri), params[0])
                            if (sdCardFolder != null) {
                                asyncDeleteSelected.deleteFolderOnSDCard(sdCardFolder, params[0])
                            }
                        }
                    } else {
                        moveState += File(element.uri).length()
                        publishProgress((moveState * 100 / selectedListSize).toInt())

                        sdCardOperations.copyToSDCard(
                            params[0].currentPath,
                            File(element.uri),
                            params[0]
                        )
                        if (element.uri.contains(params[0].rootPath.toString()))
                            File(element.uri).delete()
                        else
                            sdCardOperations.deleteOnSDCard(
                                File(
                                    element.uri.substring(
                                        0,
                                        element.uri.lastIndexOf("/")
                                    )
                                ), File(element.uri), params[0]
                            )
                    }
                } else if (params[0].currentPath.toString()
                        .contains(params[0].rootPath.toString())
                ) {
                    if (element.mimeType == FileManagerActivity.TYPE_FOLDER) {
                        if (element.uri.contains(params[0].rootPath.toString()))
                            File(element.uri).renameTo(file)
                        else {
                            moveFolder(File(element.uri), params[0])
                            val sdCardFolder =
                                sdCardOperations.getChildren(File(element.uri), params[0])
                            if (sdCardFolder != null) {
                                asyncDeleteSelected.deleteFolderOnSDCard(sdCardFolder, params[0])
                            }
                        }
                    } else {
                        moveState += File(element.uri).length()
                        publishProgress((moveState * 100 / selectedListSize).toInt())

                        if (element.uri.contains(params[0].rootPath.toString()))
                            File(element.uri).renameTo(file)
                        else {
                            File(element.uri).copyTo(file)
                            sdCardOperations.deleteOnSDCard(
                                File(
                                    element.uri.substring(
                                        0,
                                        element.uri.lastIndexOf("/")
                                    )
                                ), File(element.uri), params[0]
                            )
                        }
                    }
                }
            } else if (file.exists()) {
                params[0].runOnUiThread {
                    alertDialogMessages.alreadyExists(file.name, params[0])
                }
            } else {
                params[0].runOnUiThread {
                    alertDialogMessages.copyOrMoveIntoItself("move", params[0])
                }
            }

            if (this.isCancelled)
                break
        }
        return params[0]
    }

    override fun onProgressUpdate(vararg values: Int?) {
        progressDialog.progress = values[0]!!
    }

    override fun onPostExecute(result: FileManagerActivity) {
        progressDialog.hide()
        result.loadFiles()
    }

    override fun onCancelled(result: FileManagerActivity) {
        progressDialog.dismiss()
        result.loadFiles()
    }

    private fun moveFolder(folder: File, activity: FileManagerActivity) {
        for (src in folder.walkTopDown()) {
            val relPath = src.toRelativeString(folder.parentFile)
            val dstFile = File(activity.currentPath, relPath)

            moveState += src.length()
            publishProgress((moveState * 100 / selectedListSize).toInt())

            if (src.isDirectory)
                dstFile.mkdirs()
            else
                src.copyTo(dstFile)

            if (this.isCancelled) {
                asyncDeleteSelected.cancel(true)
                break
            }
        }
    }

    private fun moveFolderToSDCard(folder: File, activity: FileManagerActivity) {
        for (src in folder.walkTopDown()) {
            val relPath = src.toRelativeString(folder.parentFile)
            val dstFile = File(activity.currentPath, relPath)

            moveState += src.length()
            publishProgress((moveState * 100 / selectedListSize).toInt())

            if (src.isDirectory)
                sdCardOperations.createFolderOnSDCard(dstFile.parentFile, src.name, activity)
            else
                sdCardOperations.copyToSDCard(dstFile.parentFile, src, activity)

            if (this.isCancelled) {
                asyncDeleteSelected.cancel(true)
                break
            }
        }
    }
}

class AsyncSearch(private val input: String) :
    AsyncTask<FileManagerActivity, Void, MutableList<File>>() {

    override fun doInBackground(vararg params: FileManagerActivity): MutableList<File> {
        val result = mutableListOf<File>()

        params[0].currentPath.walk().forEach {
            if (it.name.toLowerCase(Locale.ROOT)
                    .contains(input.toLowerCase(Locale.ROOT)) && it.path != params[0].currentPath.toString()
            )
                result += it
        }
        return result
    }
}

private fun getFolderSize(folder: File): Double {
    var length = 0.0
    for (element in folder.listFiles()) {
        if (element.isFile)
            length += element.length()
        else
            length += getFolderSize(element)
    }
    return length
}