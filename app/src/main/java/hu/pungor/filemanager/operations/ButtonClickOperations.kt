package hu.pungor.filemanager.operations

import android.content.Intent
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
    private val fileOperations = FileOperations()
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
                activity.fileManagerAdapter.popupMenuPressed = false
            }
            .setNegativeButton(activity.getString(R.string.no)) { dialog, which ->
                if (activity.fileManagerAdapter.popupMenuPressed) {
                    activity.fileManagerAdapter.popupMenuPressed = false
                    activity.fileManagerAdapter.clearSelectedList()
                    activity.fileManagerAdapter.restoreSelectedList()
                }
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

            setButton(
                activity.create_textfile,
                activity.select_all,
                activity.delete_selected,
                activity.move_selected,
                activity = activity
            )
            activity.search.setImageResource(R.drawable.cancel)
            activity.copy_selected.setImageResource(R.drawable.ok)
        } else if (activity.fileManagerAdapter.btnCopyPressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnCopyPressed = false
            fileOperations.copySelectedFiles(activity)
            activity.fileManagerAdapter.popupMenuPressed = false

            setButton(
                activity.create_textfile,
                activity.select_all,
                activity.delete_selected,
                activity.move_selected,
                activity = activity
            )
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

            setButton(
                activity.create_textfile,
                activity.select_all,
                activity.delete_selected,
                activity.copy_selected,
                activity = activity
            )
            activity.search.setImageResource(R.drawable.cancel)
            activity.move_selected.setImageResource(R.drawable.ok)
        } else if (activity.fileManagerAdapter.btnMovePressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnMovePressed = false
            fileOperations.moveSelectedFiles(activity)
            activity.fileManagerAdapter.popupMenuPressed = false

            setButton(
                activity.create_textfile,
                activity.select_all,
                activity.delete_selected,
                activity.copy_selected,
                activity = activity
            )
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

            setButton(
                activity.create_textfile,
                activity.select_all,
                activity.delete_selected,
                activity.move_selected,
                activity = activity
            )
            activity.search.setImageResource(R.drawable.search)
            activity.copy_selected.setImageResource(R.drawable.copy)
        } else if (activity.fileManagerAdapter.btnMovePressed) {
            activity.fileManagerAdapter.clearSelectedList = true
            activity.fileManagerAdapter.btnMovePressed = false
            if (latestPathBeforeAction != activity.currentPath)
                activity.fileManagerAdapter.clearSelectedList()

            setButton(
                activity.create_textfile,
                activity.select_all,
                activity.delete_selected,
                activity.copy_selected,
                activity = activity
            )
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

                            setButton(
                                activity.create_textfile,
                                activity.create_folder,
                                activity.select_all,
                                activity.delete_selected,
                                activity.copy_selected,
                                activity.move_selected,
                                activity = activity
                            )
                            activity.search.setImageResource(R.drawable.cancel)
                        }
                    } else
                        alertDialogMessages.nameIsNull(activity)
                }
                .setNegativeButton(activity.getString(R.string.cancel), null)
            builder.show()
        } else if (activity.fileManagerAdapter.btnSearchPressed) {
            activity.currentPath = latestPathBeforeAction
            activity.fileManagerAdapter.btnSearchPressed = false
            activity.loadFiles()
            activity.Internal.isEnabled = true
            activity.SDCard.isEnabled = true

            if (activity.currentPath.toString().contains(activity.rootPath.toString())) {
                activity.Internal.backgroundTintList =
                    activity.applicationContext.resources.getColorStateList(R.color.button_pressed)
                activity.SDCard.backgroundTintList =
                    activity.applicationContext.resources.getColorStateList(R.color.button)
            } else {
                activity.Internal.backgroundTintList =
                    activity.applicationContext.resources.getColorStateList(R.color.button)
                activity.SDCard.backgroundTintList =
                    activity.applicationContext.resources.getColorStateList(R.color.button_pressed)
            }

            setButton(
                activity.create_textfile,
                activity.create_folder,
                activity.select_all,
                activity.delete_selected,
                activity.copy_selected,
                activity.move_selected,
                activity = activity
            )
            activity.search.setImageResource(R.drawable.search)
        }

        if (activity.fileManagerAdapter.popupMenuPressed) {
            activity.fileManagerAdapter.popupMenuPressed = false
            activity.fileManagerAdapter.clearSelectedList()

            if (activity.currentPath == latestPathBeforeAction)
                activity.fileManagerAdapter.setFiles(AsyncGetAllFiles().execute(activity).get())
        }
    }

    private fun setButton(vararg buttons: ImageButton, activity: FileManagerActivity) {
        for (button in buttons) {
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
    }

    fun showRationaleForStoragePermissionsBuilder(
        request: PermissionRequest,
        activity: FileManagerActivity
    ) {
        val customTitle =
            LayoutInflater.from(activity.applicationContext).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text =
            activity.getString(R.string.attention)
        val customText =
            LayoutInflater.from(activity.applicationContext)
                .inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text =
            activity.getString(R.string.rationale)

        val builder = AlertDialog.Builder(activity)
            .setView(customText)
            .setCustomTitle(customTitle)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.proceed)) { dialog, id -> request.proceed() }
            .setNegativeButton(activity.getString(R.string.exit)) { dialog, id -> request.cancel() }
            .create()
        builder.show()
    }

    fun sdCardPermissionsBuilder(activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.info
        )
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text =
            activity.getString(R.string.sdcard_permission)

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok)) { dialog, which ->
                activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
            }
        builder.show()
    }
}