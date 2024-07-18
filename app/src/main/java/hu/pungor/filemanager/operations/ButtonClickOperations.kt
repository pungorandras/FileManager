package hu.pungor.filemanager.operations

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.alertdialog.alertDialogBuilder
import hu.pungor.filemanager.alertdialog.nameIsNullDialog
import hu.pungor.filemanager.alertdialog.noItemsSelectedDialog
import hu.pungor.filemanager.alertdialog.noResultsDialog
import hu.pungor.filemanager.databinding.LayoutDialogBinding
import hu.pungor.filemanager.databinding.LayoutDialogTextfileBinding
import hu.pungor.filemanager.operations.async.listFiles
import hu.pungor.filemanager.operations.async.listFilesRunBlocking
import hu.pungor.filemanager.operations.async.resetProgressBar
import hu.pungor.filemanager.operations.async.setProgressLayoutVisibility
import hu.pungor.filemanager.permissions.checkPermissionsAndLoadFiles
import hu.pungor.filemanager.permissions.getSDCardPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import permissions.dispatcher.PermissionRequest
import java.io.File


var fileTreeDepth = 0
lateinit var result: List<File>
private lateinit var latestPathBeforeAction: File

@SuppressLint("InflateParams")
fun FileManagerActivity.createTextFileDialog() {
    val dialogViewBinding = LayoutDialogTextfileBinding.inflate(LayoutInflater.from(this))

    alertDialogBuilder(
        titleText = R.string.create_new_textfile,
        dialogLayout = dialogViewBinding.root,
        positiveButtonFunctionality = {
            createTextFile(
                dialogViewBinding.nameInput.text.toString(),
                dialogViewBinding.textInput.text.toString()
            )
        },
        negativeButtonLabel = R.string.cancel
    ).show()
}

@SuppressLint("InflateParams")
fun FileManagerActivity.createFolderDialog() {
    val dialogViewBinding = LayoutDialogBinding.inflate(LayoutInflater.from(this))

    alertDialogBuilder(
        titleText = R.string.create_new_folder,
        dialogLayout = dialogViewBinding.root,
        positiveButtonFunctionality = {
            createFolder(dialogViewBinding.nameInput.text.toString())
        },
        negativeButtonLabel = R.string.cancel
    ).show()
}

fun FileManagerActivity.selectAllOperation() {
    if (!fmAdapter.btnSelectAllPressed) {
        fmAdapter.btnSelectAllPressed = true
        fmAdapter.addAllToSelectedList()
        fmAdapter.clearSelectedList = false
        listFilesRunBlocking()
        fmAdapter.clearSelectedList = true
    } else {
        fmAdapter.btnSelectAllPressed = false
        fmAdapter.clearSelectedList()
        listFiles()
    }
}

fun FileManagerActivity.deleteSelectedDialog() {
    val selectedList = fmAdapter.getSelectedList()
    val message =
        if (selectedList.size == 1) getString(R.string.delete_item) + selectedList[0].name else getString(
            R.string.delete_selected_items
        )

    val builder = alertDialogBuilder(
        titleText = R.string.are_you_sure,
        dialogText = message,
        positiveButtonLabel = R.string.yes,
        positiveButtonFunctionality = {
            CoroutineScope(Main).launch { deleteSelectedFiles() }
            fmAdapter.popupMenuPressed = false
        },
        negativeButtonLabel = R.string.no,
        negativeButtonFunctionality = {
            if (fmAdapter.popupMenuPressed) {
                fmAdapter.popupMenuPressed = false
                fmAdapter.clearSelectedList()
                fmAdapter.restoreSelectedList()
            }
        }
    )

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
            bottomBinding.createTextfile,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.moveSelected
        )
        bottomBinding.search.setImageResource(R.drawable.cancel)
        bottomBinding.copySelected.setImageResource(R.drawable.ok)
    } else if (fmAdapter.btnCopyPressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnCopyPressed = false
        CoroutineScope(Main).launch { copySelectedFiles() }
        fmAdapter.popupMenuPressed = false

        revertButtonState(
            bottomBinding.createTextfile,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.moveSelected
        )
        bottomBinding.search.setImageResource(R.drawable.search)
        bottomBinding.copySelected.setImageResource(R.drawable.copy)
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
            bottomBinding.createTextfile,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.copySelected
        )
        bottomBinding.search.setImageResource(R.drawable.cancel)
        bottomBinding.moveSelected.setImageResource(R.drawable.ok)
    } else if (fmAdapter.btnMovePressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnMovePressed = false
        CoroutineScope(Main).launch { moveSelectedFiles() }
        fmAdapter.popupMenuPressed = false

        revertButtonState(
            bottomBinding.createTextfile,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.copySelected
        )
        bottomBinding.search.setImageResource(R.drawable.search)
        bottomBinding.moveSelected.setImageResource(R.drawable.move)
    } else
        noItemsSelectedDialog()
}

@SuppressLint("InflateParams", "UseCompatLoadingForColorStateLists")
fun FileManagerActivity.searchButtonOperations() {
    if (fmAdapter.btnCopyPressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnCopyPressed = false
        if (latestPathBeforeAction != currentPath)
            fmAdapter.clearSelectedList()

        revertButtonState(
            bottomBinding.createTextfile,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.moveSelected
        )
        bottomBinding.search.setImageResource(R.drawable.search)
        bottomBinding.copySelected.setImageResource(R.drawable.copy)
    } else if (fmAdapter.btnMovePressed) {
        fmAdapter.clearSelectedList = true
        fmAdapter.btnMovePressed = false
        if (latestPathBeforeAction != currentPath)
            fmAdapter.clearSelectedList()

        revertButtonState(
            bottomBinding.createTextfile,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.copySelected
        )
        bottomBinding.search.setImageResource(R.drawable.search)
        bottomBinding.moveSelected.setImageResource(R.drawable.move)
    } else if (!fmAdapter.btnSearchPressed) {
        val dialogViewBinding = LayoutDialogBinding.inflate(LayoutInflater.from(this))

        alertDialogBuilder(
            titleText = R.string.search,
            dialogLayout = dialogViewBinding.root,
            positiveButtonFunctionality = {
                val inputText = dialogViewBinding.nameInput.text.toString()
                if (inputText.isNotEmpty()) {
                    CoroutineScope(Main).launch {
                        latestPathBeforeAction = currentPath
                        result = search(inputText)
                        setProgressLayoutVisibility(View.GONE)
                        resetProgressBar()

                        if (result.isNullOrEmpty())
                            noResultsDialog()
                        else {
                            fmAdapter.btnSearchPressed = true
                            listFiles(result)

                            topBinding.Internal.isEnabled = false
                            topBinding.Internal.backgroundTintList =
                                resources.getColorStateList(R.color.disabled)
                            topBinding.SDCard.isEnabled = false
                            topBinding.SDCard.backgroundTintList =
                                resources.getColorStateList(R.color.disabled)

                            revertButtonState(
                                bottomBinding.createTextfile,
                                bottomBinding.createFolder,
                                bottomBinding.selectAll,
                                bottomBinding.deleteSelected,
                                bottomBinding.copySelected,
                                bottomBinding.moveSelected
                            )
                            bottomBinding.search.setImageResource(R.drawable.cancel)
                        }
                    }
                } else
                    nameIsNullDialog()
            },
            negativeButtonLabel = R.string.cancel
        ).show()
    } else {
        currentPath = latestPathBeforeAction
        fmAdapter.btnSearchPressed = false
        listFiles()
        topBinding.Internal.isEnabled = true
        topBinding.SDCard.isEnabled = true

        if (currentPath.toString().contains(rootPath.toString())) {
            topBinding.Internal.backgroundTintList =
                resources.getColorStateList(R.color.button_pressed)
            topBinding.SDCard.backgroundTintList = resources.getColorStateList(R.color.button)
        } else {
            topBinding.Internal.backgroundTintList = resources.getColorStateList(R.color.button)
            topBinding.SDCard.backgroundTintList =
                resources.getColorStateList(R.color.button_pressed)
        }

        disableSDCardButtonIfNotAvailable()

        revertButtonState(
            bottomBinding.createTextfile,
            bottomBinding.createFolder,
            bottomBinding.selectAll,
            bottomBinding.deleteSelected,
            bottomBinding.copySelected,
            bottomBinding.moveSelected
        )
        bottomBinding.search.setImageResource(R.drawable.search)
    }

    if (fmAdapter.popupMenuPressed) {
        fmAdapter.popupMenuPressed = false
        fmAdapter.clearSelectedList()

        if (currentPath == latestPathBeforeAction)
            listFiles()
    }
}

@SuppressLint("UseCompatLoadingForColorStateLists")
fun FileManagerActivity.internalButtonOperations() {
    currentPath = rootPath
    topBinding.Internal.backgroundTintList = resources.getColorStateList(R.color.button_pressed)
    if (sdCardPath != null)
        topBinding.SDCard.backgroundTintList = resources.getColorStateList(R.color.button)

    checkPermissionsAndLoadFiles()
}

@SuppressLint("UseCompatLoadingForColorStateLists")
fun FileManagerActivity.sdCardButtonOperations() {
    if (sdCardPath.toString().contains("null"))
        sdCardPath = getSDCardPath()

    if (!sdCardPath.toString().contains("null")) {
        topBinding.Internal.backgroundTintList = resources.getColorStateList(R.color.button)
        topBinding.SDCard.backgroundTintList = resources.getColorStateList(R.color.button_pressed)
        currentPath = sdCardPath!!
        checkPermissionsAndLoadFiles()
    } else
        disableSDCardButtonIfNotAvailable()
}

@SuppressLint("UseCompatLoadingForColorStateLists")
fun FileManagerActivity.disableSDCardButtonIfNotAvailable() {
    if (externalMediaDirs.size < 2) {
        topBinding.SDCard.isEnabled = false
        topBinding.SDCard.backgroundTintList =
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

fun FileManagerActivity.showRationaleForStoragePermissionsDialog(request: PermissionRequest) {
    alertDialogBuilder(
        titleText = R.string.attention,
        dialogText = R.string.rationale,
        positiveButtonLabel = R.string.proceed,
        positiveButtonFunctionality = { request.proceed() },
        negativeButtonLabel = R.string.exit,
        negativeButtonFunctionality = { request.cancel() }
    ).show()
}