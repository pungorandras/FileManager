package hu.pungor.filemanager.operations.async

import android.os.AsyncTask
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.copyOrMoveIntoItselfDialog
import hu.pungor.filemanager.operations.*
import kotlinx.coroutines.*
import java.io.File

val vcIsR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@Suppress("DEPRECATION")
class AsyncDeleteSelected(private val activity: FileManagerActivity) :
    AsyncTask<FileManagerActivity, Int, FileManagerActivity>() {

    private var deleteState = 0.0
    private var selectedListSize = 0.0
    private val progressDialog = activity.progressDialogBuilder(
        titleText = R.string.deleting,
        buttonFunctionality = { cancel(true) }
    )

    @Deprecated("Deprecated in Java")
    override fun onPreExecute() {
        progressDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: FileManagerActivity): FileManagerActivity {
        val selectedList = params[0].fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (position in selectedList.indices) {
            val fileUri = selectedList[position].path
            val file = File(fileUri)
            progressDialog.setProgressNumberFormat(((position + 1).toString() + "/" + selectedList.size))

            if (selectedList[position].mimeType == TYPE_FOLDER)
                if (params[0].currentPath.toString()
                        .contains(params[0].rootPath.toString()) || vcIsR
                )
                    deleteFolder(file)
                else {
                    val sdCardFolder =
                        params[0].getChildren(params[0].currentPath)?.findFile(file.name)
                    if (sdCardFolder != null) {
                        deleteFolderOnSDCard(sdCardFolder, params[0])
                    }
                }
            else {
                deleteState += File(selectedList[position].path).length()
                publishProgress((deleteState * 100 / selectedListSize).toInt())
                if (params[0].currentPath.toString()
                        .contains(params[0].rootPath.toString()) || vcIsR
                )
                    file.delete()
                else
                    params[0].deleteOnSDCard(params[0].currentPath, file)
            }

            if (this.isCancelled)
                break
        }
        return params[0]
    }

    @Deprecated("Deprecated in Java")
    override fun onProgressUpdate(vararg values: Int?) {
        progressDialog.progress = values[0]!!
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: FileManagerActivity) {
        progressDialog.dismiss()
        result.listFiles()
    }

    @Deprecated("Deprecated in Java")
    override fun onCancelled(result: FileManagerActivity?) {
        progressDialog.dismiss()
        result?.listFiles()
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

    private val asyncDeleteSelected = AsyncDeleteSelected(activity)
    private var moveState = 0.0
    private var selectedListSize = 0.0
    private val progressDialog = activity.progressDialogBuilder(
        titleText = R.string.moving,
        buttonFunctionality = { cancel(true) }
    )

    @Deprecated("Deprecated in Java")
    override fun onPreExecute() {
        progressDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: FileManagerActivity): FileManagerActivity {
        val selectedList = params[0].fmAdapter.getSelectedList()
        selectedListSize = getSelectedListSize(selectedList)

        for (element in selectedList) {
            val file = File(params[0].currentPath.toString() + "/" + element.name)
            progressDialog.setProgressNumberFormat(((selectedList.indexOf(element) + 1).toString() + "/" + selectedList.size))

            if (params[0].currentPath.toString() != element.path && !file.exists()) {
                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString())) {
                    if (element.mimeType == TYPE_FOLDER) {
                        if (vcIsR)
                            moveFolder(File(element.path), params[0])
                        else
                            params[0].moveFolderToSDCard(File(element.path))

                        if (element.path.contains(params[0].rootPath.toString()) || vcIsR)
                            asyncDeleteSelected.deleteFolder(File(element.path))
                        else {
                            val sdCardFolder = params[0].getChildren(File(element.path))
                            if (sdCardFolder != null) {
                                asyncDeleteSelected.deleteFolderOnSDCard(sdCardFolder, params[0])
                            }
                        }
                    } else {
                        moveState += File(element.path).length()
                        publishProgress((moveState * 100 / selectedListSize).toInt())

                        if (vcIsR)
                            File(element.path).copyTo(file)
                        else
                            params[0].copyToSDCard(params[0].currentPath, File(element.path))

                        if (element.path.contains(params[0].rootPath.toString()) || vcIsR)
                            File(element.path).delete()
                        else
                            params[0].deleteOnSDCard(
                                File(
                                    element.path.substring(
                                        0,
                                        element.path.lastIndexOf("/")
                                    )
                                ), File(element.path)
                            )
                    }
                } else if (params[0].currentPath.toString()
                        .contains(params[0].rootPath.toString())
                ) {
                    if (element.mimeType == TYPE_FOLDER) {
                        if (element.path.contains(params[0].rootPath.toString()))
                            File(element.path).renameTo(file)
                        else {
                            moveFolder(File(element.path), params[0])
                            if (vcIsR)
                                asyncDeleteSelected.deleteFolder(File(element.path))
                            else {
                                val sdCardFolder = params[0].getChildren(File(element.path))
                                if (sdCardFolder != null) {
                                    asyncDeleteSelected.deleteFolderOnSDCard(
                                        sdCardFolder,
                                        params[0]
                                    )
                                }
                            }
                        }
                    } else {
                        moveState += File(element.path).length()
                        publishProgress((moveState * 100 / selectedListSize).toInt())

                        if (element.path.contains(params[0].rootPath.toString()))
                            File(element.path).renameTo(file)
                        else {
                            File(element.path).copyTo(file)
                            if (vcIsR)
                                File(element.path).delete()
                            else {
                                params[0].deleteOnSDCard(
                                    File(
                                        element.path.substring(
                                            0,
                                            element.path.lastIndexOf("/")
                                        )
                                    ), File(element.path)
                                )
                            }
                        }
                    }
                }
            } else if (file.exists()) {
                params[0].runOnUiThread {
                    params[0].alreadyExistsDialog(file.name)
                }
            } else {
                params[0].runOnUiThread {
                    params[0].copyOrMoveIntoItselfDialog("move")
                }
            }

            if (this.isCancelled)
                break
        }
        return params[0]
    }

    @Deprecated("Deprecated in Java")
    override fun onProgressUpdate(vararg values: Int?) {
        progressDialog.progress = values[0]!!
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: FileManagerActivity) {
        progressDialog.dismiss()
        result.listFiles()
    }

    @Deprecated("Deprecated in Java")
    override fun onCancelled(result: FileManagerActivity) {
        progressDialog.dismiss()
        result.listFiles()
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

    private fun FileManagerActivity.moveFolderToSDCard(folder: File) {
        for (src in folder.walkTopDown()) {
            val relPath = src.toRelativeString(folder.parentFile)
            val dstFile = File(activity.currentPath, relPath)

            moveState += src.length()
            publishProgress((moveState * 100 / selectedListSize).toInt())

            if (src.isDirectory)
                createFolderOnSDCard(dstFile.parentFile, src.name)
            else
                copyToSDCard(dstFile.parentFile, src)

            if (isCancelled) {
                asyncDeleteSelected.cancel(true)
                break
            }
        }
    }
}

