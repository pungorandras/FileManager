package hu.pungor.filemanager.operations

import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.AlertDialogMessages
import kotlinx.android.synthetic.main.activity_filemanager.*
import permissions.dispatcher.PermissionRequest
import java.io.File

@Suppress("DEPRECATION")
class ButtonClickOperations(activity: FileManagerActivity) {
    private val fileOperations = FileOperations(activity)
    private val alertDialogMessages =
        AlertDialogMessages()

    var fileTreeDepth = 0
    lateinit var result: List<File>
    private lateinit var latestPathBeforeAction: File

    fun createTextFileBuilder(activity: FileManagerActivity) {
        val dialogView =
            LayoutInflater.from(activity.applicationContext)
                .inflate(R.layout.layout_dialog_textfile, null)
        val customTitle =
            LayoutInflater.from(activity.applicationContext).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text =
            activity.getString(R.string.create_new_textfile)

        val builder = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->
                fileOperations.createTextFile(
                    dialogView.findViewById<EditText>(R.id.name_input).text.toString(),
                    dialogView.findViewById<EditText>(R.id.text_input).text.toString(),
                    activity
                )
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
        builder.show()
    }

    fun createFolderBuilder(activity: FileManagerActivity) {
        val dialogView =
            LayoutInflater.from(activity.applicationContext).inflate(R.layout.layout_dialog, null)
        val customTitle =
            LayoutInflater.from(activity.applicationContext).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text =
            activity.getString(R.string.create_new_folder)

        val builder = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->
                fileOperations.createFolder(
                    dialogView.findViewById<EditText>(R.id.name_input).text.toString(),
                    activity
                )
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
        builder.show()
    }

    fun selectAllOperation(activity: FileManagerActivity) {
        if (!activity.fileManagerAdapter.btnSelectAllPressed) {
            activity.fileManagerAdapter.btnSelectAllPressed = true
            activity.fileManagerAdapter.addAllToSelectedList()
            activity.fileManagerAdapter.clearSelectedList = false
            activity.loadFiles()
            activity.fileManagerAdapter.clearSelectedList = true
        } else {
            activity.fileManagerAdapter.btnSelectAllPressed = false
            activity.fileManagerAdapter.clearSelectedList()
            activity.loadFiles()
        }
    }

    fun deleteSelectedBuilder(activity: FileManagerActivity) {
        val selectedList = activity.fileManagerAdapter.getSelectedList()
        val message =
            if (selectedList.size == 1) activity.getString(R.string.delete_item) + selectedList[0].name else activity.getString(
                R.string.delete_selected_items
            )

        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text =
            activity.getString(R.string.are_you_sure)
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text = message

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.yes)) { dialog, which ->
                fileOperations.deleteSelectedFiles(activity)
            }
            .setNegativeButton(activity.getString(R.string.no)) { dialog, which ->
                activity.fileManagerAdapter.clearSelectedList()
            }

        if (!selectedList.isNullOrEmpty())
            builder.show()
        else
            alertDialogMessages.noItemsSelected(activity)
    }

    fun copySelectedOperation(activity: FileManagerActivity) {
        val selectedList = activity.fileManagerAdapter.getSelectedList()

        if (!activity.fileManagerAdapter.btnMovePressed && !activity.fileManagerAdapter.btnCopyPressed && !selectedList.isNullOrEmpty()) {
            activity.fileManagerAdapter.clearSelectedList = false
            activity.fileManagerAdapter.btnCopyPressed = true
            latestPathBeforeAction = activity.currentPath

            setButton(activity.create_textfile, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.move_selected, activity)
            activity.search.setImageResource(R.drawable.cancel)
            activity.copy_selected.setImageResource(R.drawable.ok)
        } else if (activity.fileManagerAdapter.btnCopyPressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnCopyPressed = false

            fileOperations.copySelectedFiles(activity)

            setButton(activity.create_textfile, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.move_selected, activity)
            activity.search.setImageResource(R.drawable.search)
            activity.copy_selected.setImageResource(R.drawable.copy)
        } else
            alertDialogMessages.noItemsSelected(activity)
    }

    fun moveSelectedOperation(activity: FileManagerActivity) {
        val selectedList = activity.fileManagerAdapter.getSelectedList()

        if (!activity.fileManagerAdapter.btnCopyPressed && !activity.fileManagerAdapter.btnMovePressed && !selectedList.isNullOrEmpty()) {
            activity.fileManagerAdapter.clearSelectedList = false
            activity.fileManagerAdapter.btnMovePressed = true
            latestPathBeforeAction = activity.currentPath

            setButton(activity.create_textfile, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.copy_selected, activity)
            activity.search.setImageResource(R.drawable.cancel)
            activity.move_selected.setImageResource(R.drawable.ok)
        } else if (activity.fileManagerAdapter.btnMovePressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnMovePressed = false

            fileOperations.moveSelectedFiles(activity)

            setButton(activity.create_textfile, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.copy_selected, activity)
            activity.search.setImageResource(R.drawable.search)
            activity.move_selected.setImageResource(R.drawable.move)
        } else
            alertDialogMessages.noItemsSelected(activity)
    }

    fun searchButtonOperations(activity: FileManagerActivity) {
        if (activity.fileManagerAdapter.btnCopyPressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnCopyPressed = false

            if (latestPathBeforeAction != activity.currentPath)
                activity.fileManagerAdapter.clearSelectedList()

            setButton(activity.create_textfile, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.move_selected, activity)
            activity.search.setImageResource(R.drawable.search)
            activity.copy_selected.setImageResource(R.drawable.copy)
        } else if (activity.fileManagerAdapter.btnMovePressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnMovePressed = false

            if (latestPathBeforeAction != activity.currentPath)
                activity.fileManagerAdapter.clearSelectedList()

            setButton(activity.create_textfile, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.copy_selected, activity)
            activity.search.setImageResource(R.drawable.search)
            activity.move_selected.setImageResource(R.drawable.move)
        } else if (!activity.fileManagerAdapter.btnSearchPressed) {
            val dialogView = LayoutInflater.from(activity.applicationContext)
                .inflate(R.layout.layout_dialog, null)
            val customTitle =
                LayoutInflater.from(activity.applicationContext)
                    .inflate(R.layout.custom_title, null)
            customTitle.findViewById<TextView>(R.id.title_text).text =
                activity.getString(R.string.search)

            val builder = AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCustomTitle(customTitle)
                .setCancelable(false)
                .setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->

                    if (dialogView.findViewById<EditText>(R.id.name_input).text.toString()
                            .isNotEmpty()
                    ) {
                        latestPathBeforeAction = activity.currentPath
                        result =
                            fileOperations.search(
                                dialogView.findViewById<EditText>(R.id.name_input).text.toString(),
                                activity
                            )

                        if (result.isNullOrEmpty())
                            alertDialogMessages.noResults(activity)
                        else {
                            activity.fileManagerAdapter.btnSearchPressed = true
                            activity.rvFiles.adapter = activity.fileManagerAdapter
                            activity.fileManagerAdapter.setFiles(activity.fillList(result))

                            activity.Internal.isEnabled = false
                            activity.Internal.backgroundTintList =
                                activity.applicationContext.resources.getColorStateList(R.color.disabled)
                            activity.SDCard.isEnabled = false
                            activity.SDCard.backgroundTintList =
                                activity.applicationContext.resources.getColorStateList(R.color.disabled)

                            setButton(activity.create_textfile, activity)
                            setButton(activity.create_folder, activity)
                            setButton(activity.select_all, activity)
                            setButton(activity.delete_selected, activity)
                            setButton(activity.copy_selected, activity)
                            setButton(activity.move_selected, activity)
                            activity.search.setImageResource(R.drawable.cancel)
                        }
                    } else
                        alertDialogMessages.nameIsNull(activity)
                }
                .setNegativeButton(activity.getString(R.string.cancel), null)
            builder.show()
        } else {
            activity.currentPath = latestPathBeforeAction
            activity.fileManagerAdapter.btnSearchPressed = false
            activity.loadFiles()
            activity.Internal.isEnabled = true

            if (activity.currentPath.toString().contains(activity.rootPath.toString()))
                activity.Internal.backgroundTintList =
                    activity.applicationContext.resources.getColorStateList(R.color.button_pressed)
            else
                activity.Internal.backgroundTintList =
                    activity.applicationContext.resources.getColorStateList(R.color.button)

            if (activity.sdCardPath != null) {
                activity.SDCard.isEnabled = true

                if (activity.currentPath.toString().contains(activity.sdCardPath.toString()))
                    activity.SDCard.backgroundTintList =
                        activity.applicationContext.resources.getColorStateList(R.color.button_pressed)
                else
                    activity.SDCard.backgroundTintList =
                        activity.applicationContext.resources.getColorStateList(R.color.button)
            }

            setButton(activity.create_textfile, activity)
            setButton(activity.create_folder, activity)
            setButton(activity.select_all, activity)
            setButton(activity.delete_selected, activity)
            setButton(activity.copy_selected, activity)
            setButton(activity.move_selected, activity)
            activity.search.setImageResource(R.drawable.search)
        }
    }

    private fun setButton(button: ImageButton, activity: FileManagerActivity) {
        if (button.isEnabled) {
            button.isEnabled = false
            button.backgroundTintList =
                activity.applicationContext.resources.getColorStateList(R.color.disabled)
        } else {
            button.isEnabled = true
            button.backgroundTintList =
                activity.applicationContext.resources.getColorStateList(R.color.button)
        }
    }

    fun showRationaleForStoragePermissionsBuilder(
        request: PermissionRequest,
        activity: FileManagerActivity
    ) {
        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.attention))
            .setMessage(activity.getString(R.string.rationale))
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.proceed)) { dialog, id -> request.proceed() }
            .setNegativeButton(activity.getString(R.string.exit)) { dialog, id -> request.cancel() }
            .create()
        alertDialog.show()
    }
}