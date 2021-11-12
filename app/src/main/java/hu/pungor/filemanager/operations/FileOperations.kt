package hu.pungor.filemanager.operations

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.AlertDialogMessages
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.permissions.StoragePermissions
import java.io.File
import java.io.FileWriter

class FileOperations {

    private val alertDialogMessages = AlertDialogMessages()
    private val sdCardOperations = SDCardOperationsUntilApi29()
    private val storagePermissions = StoragePermissions()
    private val versionCodeIsR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun openFile(file: AboutFile, activity: FileManagerActivity) {
        val intent = Intent(Intent.ACTION_VIEW)

        if (activity.currentPath.toString().contains(activity.sdCardPath.toString())) {
            val uri = sdCardOperations.getChildren(activity.currentPath, activity)
                ?.findFile(file.name)?.uri
            intent.setDataAndType(uri, file.mimeType)
        } else {
            intent.setDataAndType(
                FileProvider.getUriForFile(
                    activity.applicationContext,
                    activity.applicationContext.packageName + ".provider",
                    File(file.path)
                ), file.mimeType
            )
        }

        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        activity.startActivity(intent)
    }

    fun openFolder(
        file: AboutFile,
        activity: FileManagerActivity,
        operations: ButtonClickOperations
    ) {
        if (file.mimeType == FileManagerActivity.TYPE_FOLDER) {
            if (activity.fileManagerAdapter.btnSearchPressed)
                operations.fileTreeDepth++

            activity.currentPath = File(file.path)
            activity.loadFiles()
        }
    }

    fun openUnknown(file: AboutFile, activity: FileManagerActivity) {
        val intent = Intent(Intent.ACTION_VIEW)

        intent.setDataAndType(
            FileProvider.getUriForFile(
                activity.applicationContext, activity.applicationContext.packageName + ".provider",
                File(file.path)
            ), "*/*"
        )

        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        activity.startActivity(intent)
    }

    fun createTextFile(name: String, notes: String, activity: FileManagerActivity) {
        if (activity.currentPath.toString()
                .contains(activity.rootPath.toString()) || versionCodeIsR
        ) {
            try {
                val file = File(activity.currentPath.toString() + "/" + name + ".txt")

                if (!file.exists() && name.isNotEmpty()) {
                    val textFile = File(activity.currentPath, "$name.txt")
                    val fileWriter = FileWriter(textFile)
                    fileWriter.append(notes)
                    fileWriter.flush()
                    fileWriter.close()
                    activity.loadFiles()
                } else if (name.isEmpty())
                    alertDialogMessages.nameIsNull(activity)
                else
                    alertDialogMessages.alreadyExists(name, activity)
            } catch (e: Exception) {
                storagePermissions.checkPermissionsAndLoadFiles(activity)
            }
        } else {
            sdCardOperations.createTextFileOnSDCard(name, notes, activity)
            storagePermissions.checkPermissionsAndLoadFiles(activity)
        }
    }

    fun createFolder(name: String, activity: FileManagerActivity) {
        if (activity.currentPath.toString()
                .contains(activity.rootPath.toString()) || versionCodeIsR
        ) {
            try {
                val folder = File(activity.currentPath, name)
                if (!folder.exists()) {
                    folder.mkdir()
                    activity.loadFiles()
                } else if (name.isEmpty())
                    alertDialogMessages.nameIsNull(activity)
                else
                    alertDialogMessages.alreadyExists(name, activity)
            } catch (e: Exception) {
                storagePermissions.checkPermissionsAndLoadFiles(activity)
            }
        } else {
            sdCardOperations.createFolderOnSDCard(activity.currentPath, name, activity)
            storagePermissions.checkPermissionsAndLoadFiles(activity)
        }
    }

    fun shareFile(view: View, position: Int, activity: FileManagerActivity) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(
            Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                view.context, view.context.packageName + ".provider",
                File(activity.fileManagerAdapter.getItem(position).path)
            )
        )
        intent.type = "*/*"
        view.context.startActivity(intent)
    }

    fun renameFile(view: View, position: Int, activity: FileManagerActivity) {
        val currentItem = activity.fileManagerAdapter.getItem(position)
        val dialogView = LayoutInflater.from(view.context).inflate(R.layout.layout_dialog, null)
        dialogView.findViewById<EditText>(R.id.name_input).setText(currentItem.name)

        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text =
            activity.getString(R.string.rename)

        val builder = AlertDialog.Builder(view.context)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->
                val file = File(currentItem.path)
                val newName = dialogView.findViewById<EditText>(R.id.name_input).text.toString()

                if (newName.isNotEmpty()) {
                    if (activity.currentPath.toString()
                            .contains(activity.sdCardPath.toString()) || versionCodeIsR
                    ) {
                        file.renameTo(
                            File(
                                activity.currentPath,
                                newName
                            )
                        )
                        activity.loadFiles()
                    } else {
                        sdCardOperations.renameOnSDCard(currentItem, newName, activity)
                        activity.loadFiles()
                    }
                } else
                    alertDialogMessages.nameIsNull(activity)
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
        builder.show()
    }

    fun deleteSelectedFiles(activity: FileManagerActivity) {
        AsyncDeleteSelected(activity).execute(activity)
    }

    fun copySelectedFiles(activity: FileManagerActivity) {
        AsyncCopySelected(activity).execute(activity)
    }

    fun moveSelectedFiles(activity: FileManagerActivity) {
        AsyncMoveSelected(activity).execute(activity)
    }

    fun search(input: String, activity: FileManagerActivity): List<File> {
        return AsyncSearch(input, activity).execute(activity).get()
    }
}