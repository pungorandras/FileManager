package hu.pungor.filemanager.operations

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alreadyExistsDialog
import hu.pungor.filemanager.alertdialog.copyOrMoveIntoItselfDialog
import kotlinx.coroutines.*
import java.io.File
import java.util.*

private val vcIsR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

suspend fun FileManagerActivity.asyncGetAllFiles() {
    val fileList = CoroutineScope(Dispatchers.IO).async {
        return@async currentPath.listFiles()?.asList()?.let { fillList(it) }
    }
    fileList.await()?.let { fmAdapter.setFiles(it) }
}

@OptIn(DelicateCoroutinesApi::class)
fun FileManagerActivity.setFiles(fileList: List<File>? = null) {
    if (fileList == null)
        GlobalScope.launch(Dispatchers.Main) { asyncGetAllFiles() }
    else
        GlobalScope.launch(Dispatchers.Main) { fmAdapter.setFiles(fillList(fileList)) }
}

fun FileManagerActivity.setFilesRunBlocking() {
    runBlocking { asyncGetAllFiles() }
}

@Suppress("DEPRECATION")
fun FileManagerActivity.progressDialogBuilder(
    titleText: Int,
    message: SpannableStringBuilder? = null,
    progressStyle: Int = ProgressDialog.STYLE_HORIZONTAL,
    buttonFunctionality: (() -> Unit)
): ProgressDialog {
    val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = getString(titleText)

    return ProgressDialog(this).apply {
        setCustomTitle(customTitle)
        setProgressStyle(progressStyle)
        setMessage(message)
        max = 100
        progress = 0
        setCancelable(false)
        setButton(
            DialogInterface.BUTTON_NEGATIVE,
            getString(R.string.cancel)
        ) { _, _ -> buttonFunctionality.invoke() }
    }
}

@Suppress("DEPRECATION")
class AsyncCopySelected(private val activity: FileManagerActivity) :
    AsyncTask<FileManagerActivity, Int, FileManagerActivity>() {

    private var copyState = 0.0
    private var selectedListSize = 0.0
    private val progressDialog = activity.progressDialogBuilder(
        titleText = R.string.copying,
        buttonFunctionality = { cancel(true) }
    )

    @Deprecated("Deprecated in Java")
    override fun onPreExecute() {
        progressDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: FileManagerActivity): FileManagerActivity? {
        val selectedList = params[0].fmAdapter.getSelectedList()
        selectedList.forEach {
            if (it.mimeType == FileManagerActivity.TYPE_FOLDER)
                selectedListSize += getFolderSize(File(it.path))
            else
                selectedListSize += File(it.path).length()
        }

        for (element in selectedList) {
            val file = File(params[0].currentPath.toString() + "/" + element.name)
            progressDialog.setProgressNumberFormat(((selectedList.indexOf(element) + 1).toString() + "/" + selectedList.size))

            if (element.mimeType == FileManagerActivity.TYPE_FOLDER && !params[0].currentPath.toString()
                    .contains(element.path) && !file.exists()
            ) {
                if (params[0].currentPath.toString()
                        .contains(params[0].rootPath.toString()) || vcIsR
                )
                    copyFolder(File(element.path), params[0])
                else
                    params[0].copyFolderToSDCard(File(element.path))

            } else if (!file.exists() && !params[0].currentPath.toString().contains(element.path)) {
                copyState += File(element.path).length()
                publishProgress((copyState * 100 / selectedListSize).toInt())
                if (params[0].currentPath.toString()
                        .contains(params[0].rootPath.toString()) || vcIsR
                )
                    File(element.path).copyTo(file)
                else
                    params[0].copyToSDCard(
                        params[0].currentPath,
                        File(element.path)
                    )
            } else if (file.exists()) {
                params[0].runOnUiThread {
                    params[0].alreadyExistsDialog(element.name)
                }
            } else {
                params[0].runOnUiThread {
                    params[0].copyOrMoveIntoItselfDialog("copy")
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
        result.loadFiles()
    }

    @Deprecated("Deprecated in Java")
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

    private fun FileManagerActivity.copyFolderToSDCard(folder: File) {
        for (src in folder.walkTopDown()) {
            val relPath = src.toRelativeString(folder.parentFile)
            val dstFile = File(activity.currentPath, relPath)

            copyState += src.length()
            publishProgress((copyState * 100 / selectedListSize).toInt())

            if (src.isDirectory)
                createFolderOnSDCard(dstFile.parentFile, src.name)
            else
                copyToSDCard(dstFile.parentFile, src)

            if (isCancelled)
                break
        }
    }
}

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
        val selectedList = params[0].fmAdapter.getSelectedList().toMutableList()
        for (i in selectedList)
            selectedList.forEach {
                if (it.mimeType == FileManagerActivity.TYPE_FOLDER)
                    selectedListSize += getFolderSize(File(it.path))
                else
                    selectedListSize += File(it.path).length()
            }

        for (position in selectedList.indices) {
            val fileUri = selectedList[position].path
            val file = File(fileUri)
            progressDialog.setProgressNumberFormat(((position + 1).toString() + "/" + selectedList.size))

            if (selectedList[position].mimeType == FileManagerActivity.TYPE_FOLDER)
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
        result.loadFiles()
    }

    @Deprecated("Deprecated in Java")
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
        selectedList.forEach {
            if (it.mimeType == FileManagerActivity.TYPE_FOLDER)
                selectedListSize += getFolderSize(File(it.path))
            else
                selectedListSize += File(it.path).length()
        }

        for (element in selectedList) {
            val file = File(params[0].currentPath.toString() + "/" + element.name)
            progressDialog.setProgressNumberFormat(((selectedList.indexOf(element) + 1).toString() + "/" + selectedList.size))

            if (params[0].currentPath.toString() != element.path && !file.exists()) {
                if (params[0].currentPath.toString().contains(params[0].sdCardPath.toString())) {
                    if (element.mimeType == FileManagerActivity.TYPE_FOLDER) {
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
                    if (element.mimeType == FileManagerActivity.TYPE_FOLDER) {
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
        result.loadFiles()
    }

    @Deprecated("Deprecated in Java")
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

@Suppress("DEPRECATION")
class AsyncSearch(private val input: String, private val activity: FileManagerActivity) :
    AsyncTask<FileManagerActivity, Void, MutableList<File>>() {

    private val sBuilder = SpannableStringBuilder(activity.getString(R.string.wait)).apply {
        setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            0,
            length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        setSpan(
            AbsoluteSizeSpan(activity.resources.getDimensionPixelSize(R.dimen.wait)),
            0,
            length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    }

    private val progressDialog = activity.progressDialogBuilder(
        titleText = R.string.searching,
        message = sBuilder,
        progressStyle = ProgressDialog.STYLE_SPINNER,
        buttonFunctionality = { cancel(true) }
    )

    @Deprecated("Deprecated in Java")
    override fun onPreExecute() {
        progressDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: FileManagerActivity): MutableList<File> {
        val result = mutableListOf<File>()

        params[0].currentPath.walk().takeWhile { !this.isCancelled }.forEach {
            if (it.name.lowercase(Locale.ROOT)
                    .contains(input.lowercase(Locale.ROOT)) && it.path != params[0].currentPath.toString()
            )
                result += it
        }
        return result
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: MutableList<File>?) {
        progressDialog.dismiss()
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