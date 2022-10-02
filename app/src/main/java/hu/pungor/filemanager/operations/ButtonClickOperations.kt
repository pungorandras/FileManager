package hu.pungor.filemanager.operations

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.nameIsNullDialog
import hu.pungor.filemanager.alertdialog.noItemsSelectedDialog
import hu.pungor.filemanager.alertdialog.noResultsDialog
import hu.pungor.filemanager.permissions.checkPermissionsAndLoadFiles
import hu.pungor.filemanager.permissions.getSDCardPath
import kotlinx.android.synthetic.main.activity_filemanager.*
import permissions.dispatcher.PermissionRequest
import java.io.File


var fileTreeDepth = 0
lateinit var result: List<File>
private lateinit var latestPathBeforeAction: File

@SuppressLint("InflateParams")
fun FileManagerActivity.createTextFileBuilder() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.layout_dialog_textfile, null)
    val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text =
        getString(R.string.create_new_textfile)

    val builder = AlertDialog.Builder(this)
        .setView(dialogView)
        .setCustomTitle(customTitle)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
            createTextFile(
                dialogView.findViewById<EditText>(R.id.name_input).text.toString(),
                dialogView.findViewById<EditText>(R.id.text_input).text.toString()
            )
        }
        .setNegativeButton(getString(R.string.cancel), null)
    builder.show()
}

@SuppressLint("InflateParams")
fun FileManagerActivity.createFolderBuilder() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null)
    val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = getString(R.string.create_new_folder)

    val builder = AlertDialog.Builder(this)
        .setView(dialogView)
        .setCustomTitle(customTitle)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
            createFolder(dialogView.findViewById<EditText>(R.id.name_input).text.toString())
        }
        .setNegativeButton(getString(R.string.cancel), null)
    builder.show()
}

fun FileManagerActivity.selectAllOperation() {
    if (!fmAdapter.btnSelectAllPressed) {
        fmAdapter.btnSelectAllPressed = true
        fmAdapter.addAllToSelectedList()
        fmAdapter.clearSelectedList = false
        loadFiles()
        fmAdapter.clearSelectedList = true
    } else {
        fmAdapter.btnSelectAllPressed = false
        fmAdapter.clearSelectedList()
        loadFiles()
    }
}

@SuppressLint("InflateParams")
fun FileManagerActivity.deleteSelectedBuilder() {
    val selectedList = fmAdapter.getSelectedList()
    val message =
        if (selectedList.size == 1) getString(R.string.delete_item) + selectedList[0].name else getString(
            R.string.delete_selected_items
        )

    val customTitle =
        LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = getString(R.string.are_you_sure)
    val customText = LayoutInflater.from(this).inflate(R.layout.custom_text_alertdialog, null)
    customText.findViewById<TextView>(R.id.custom_text).text = message

    val builder = AlertDialog.Builder(this)
        .setCustomTitle(customTitle)
        .setView(customText)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.yes)) { dialog, which ->
            deleteSelectedFiles()
            fmAdapter.popupMenuPressed = false
        }
        .setNegativeButton(getString(R.string.no)) { dialog, which ->
            if (fmAdapter.popupMenuPressed) {
                fmAdapter.popupMenuPressed = false
                fmAdapter.clearSelectedList()
                fmAdapter.restoreSelectedList()
            }
        }

    if (!selectedList.isNullOrEmpty())
        builder.show()
    else
        noItemsSelectedDialog()
}

fun FileManagerActivity.copySelectedOperation() {
    val selectedList = fmAdapter.getSelectedList()

    if (!fmAdapter.btnMovePressed && !fmAdapter.btnCopyPressed && !selectedList.isNullOrEmpty()) {
        fmAdapter.clearSelectedList = false
        fmAdapter.btnCopyPressed = true
        latestPathBeforeAction = currentPath

        revertButtonState(
            create_textfile,
            select_all,
            delete_selected,
            move_selected
        )
        search.setImageResource(R.drawable.cancel)
        copy_selected.setImageResource(R.drawable.ok)
    } else if (fmAdapter.btnCopyPressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnCopyPressed = false
        copySelectedFiles()
        fmAdapter.popupMenuPressed = false

        revertButtonState(
            create_textfile,
            select_all,
            delete_selected,
            move_selected
        )
        search.setImageResource(R.drawable.search)
        copy_selected.setImageResource(R.drawable.copy)
    } else
        noItemsSelectedDialog()
}

fun FileManagerActivity.moveSelectedOperation() {
    val selectedList = fmAdapter.getSelectedList()

    if (!fmAdapter.btnCopyPressed && !fmAdapter.btnMovePressed && !selectedList.isNullOrEmpty()) {
        fmAdapter.clearSelectedList = false
        fmAdapter.btnMovePressed = true
        latestPathBeforeAction = currentPath

        revertButtonState(
            create_textfile,
            select_all,
            delete_selected,
            copy_selected
        )
        search.setImageResource(R.drawable.cancel)
        move_selected.setImageResource(R.drawable.ok)
    } else if (fmAdapter.btnMovePressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnMovePressed = false
        moveSelectedFiles()
        fmAdapter.popupMenuPressed = false

        revertButtonState(
            create_textfile,
            select_all,
            delete_selected,
            copy_selected
        )
        search.setImageResource(R.drawable.search)
        move_selected.setImageResource(R.drawable.move)
    } else
        noItemsSelectedDialog()
}

@SuppressLint("UseCompatLoadingForColorStateLists", "InflateParams")
fun FileManagerActivity.searchButtonOperations() {
    if (fmAdapter.btnCopyPressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnCopyPressed = false
        if (latestPathBeforeAction != currentPath)
            fmAdapter.clearSelectedList()

        revertButtonState(
            create_textfile,
            select_all,
            delete_selected,
            move_selected
        )
        search.setImageResource(R.drawable.search)
        copy_selected.setImageResource(R.drawable.copy)
    } else if (fmAdapter.btnMovePressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnMovePressed = false
        if (latestPathBeforeAction != currentPath)
            fmAdapter.clearSelectedList()

        revertButtonState(
            create_textfile,
            select_all,
            delete_selected,
            copy_selected
        )
        search.setImageResource(R.drawable.search)
        move_selected.setImageResource(R.drawable.move)
    } else if (!fmAdapter.btnSearchPressed) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null)
        val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = getString(R.string.search)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, which ->

                if (dialogView.findViewById<EditText>(R.id.name_input).text.toString()
                        .isNotEmpty()
                ) {
                    latestPathBeforeAction = currentPath
                    result =
                        search(dialogView.findViewById<EditText>(R.id.name_input).text.toString())

                    if (result.isNullOrEmpty())
                        noResultsDialog()
                    else {
                        fmAdapter.btnSearchPressed = true
                        rvFiles.adapter = fmAdapter
                        fmAdapter.setFiles(fillList(result))

                        Internal.isEnabled = false
                        Internal.backgroundTintList = resources.getColorStateList(R.color.disabled)
                        SDCard.isEnabled = false
                        SDCard.backgroundTintList = resources.getColorStateList(R.color.disabled)

                        revertButtonState(
                            create_textfile,
                            create_folder,
                            select_all,
                            delete_selected,
                            copy_selected,
                            move_selected
                        )
                        search.setImageResource(R.drawable.cancel)
                    }
                } else
                    nameIsNullDialog()
            }
            .setNegativeButton(getString(R.string.cancel), null)
        builder.show()
    } else if (fmAdapter.btnSearchPressed) {
        currentPath = latestPathBeforeAction
        fmAdapter.btnSearchPressed = false
        loadFiles()
        Internal.isEnabled = true
        SDCard.isEnabled = true

        if (currentPath.toString().contains(rootPath.toString())) {
            Internal.backgroundTintList = resources.getColorStateList(R.color.button_pressed)
            SDCard.backgroundTintList = resources.getColorStateList(R.color.button)
        } else {
            Internal.backgroundTintList = resources.getColorStateList(R.color.button)
            SDCard.backgroundTintList = resources.getColorStateList(R.color.button_pressed)
        }

        disableSDCardButtonIfNotAvailable()

        revertButtonState(
            create_textfile,
            create_folder,
            select_all,
            delete_selected,
            copy_selected,
            move_selected
        )
        search.setImageResource(R.drawable.search)
    }

    if (fmAdapter.popupMenuPressed) {
        fmAdapter.popupMenuPressed = false
        fmAdapter.clearSelectedList()

        if (currentPath == latestPathBeforeAction)
            fmAdapter.setFiles(AsyncGetAllFiles().execute().get())
    }
}

@SuppressLint("UseCompatLoadingForColorStateLists")
fun FileManagerActivity.internalButtonOperations() {
    currentPath = rootPath
    Internal.backgroundTintList = resources.getColorStateList(R.color.button_pressed)
    if (sdCardPath != null)
        SDCard.backgroundTintList = resources.getColorStateList(R.color.button)

    checkPermissionsAndLoadFiles()
}

@SuppressLint("UseCompatLoadingForColorStateLists")
fun FileManagerActivity.sdCardButtonOperations() {
    sdCardPath = getSDCardPath()

    if (!sdCardPath.toString().contains("null")) {
        Internal.backgroundTintList = resources.getColorStateList(R.color.button)
        SDCard.backgroundTintList = resources.getColorStateList(R.color.button_pressed)
        currentPath = sdCardPath!!
        checkPermissionsAndLoadFiles()
    } else
        disableSDCardButtonIfNotAvailable()
}

@SuppressLint("UseCompatLoadingForColorStateLists")
fun FileManagerActivity.disableSDCardButtonIfNotAvailable() {
    if (externalMediaDirs.size < 2) {
        SDCard.isEnabled = false
        SDCard.backgroundTintList =
            resources.getColorStateList(R.color.disabled)
    }
}

@SuppressLint("UseCompatLoadingForColorStateLists")
private fun FileManagerActivity.revertButtonState(vararg buttons: ImageButton) {
    for (button in buttons) {
        if (button.isEnabled) {
            button.isEnabled = false
            button.backgroundTintList = resources.getColorStateList(R.color.disabled)
        } else {
            button.isEnabled = true
            button.backgroundTintList = resources.getColorStateList(R.color.button)
        }
    }
}

@SuppressLint("InflateParams")
fun FileManagerActivity.showRationaleForStoragePermissionsBuilder(request: PermissionRequest) {
    val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = getString(R.string.attention)
    val customText = LayoutInflater.from(this).inflate(R.layout.custom_text_alertdialog, null)
    customText.findViewById<TextView>(R.id.custom_text).text = getString(R.string.rationale)

    val builder = AlertDialog.Builder(this)
        .setView(customText)
        .setCustomTitle(customTitle)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.proceed)) { dialog, id -> request.proceed() }
        .setNegativeButton(getString(R.string.exit)) { dialog, id -> request.cancel() }
        .create()
    builder.show()
}