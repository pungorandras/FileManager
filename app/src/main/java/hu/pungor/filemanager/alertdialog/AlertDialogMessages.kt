package hu.pungor.filemanager.alertdialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R

@SuppressLint("InflateParams")
fun FileManagerActivity.alertDialogBuilder(
    titleText: Any,
    dialogText: Any? = null,
    dialogLayout: Any = R.layout.custom_text_alertdialog,
    positiveButtonLabel: Int = R.string.ok,
    positiveButtonFunctionality: (() -> Unit)? = null,
    negativeButtonLabel: Int? = null,
    negativeButtonFunctionality: (() -> Unit)? = null
): AlertDialog.Builder {
    val titleTextView = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    titleTextView.findViewById<TextView>(R.id.title_text).text =
        if (titleText is Int) getString(titleText) else titleText as CharSequence

    val dialogView = if (dialogLayout is Int) LayoutInflater.from(this)
        .inflate(dialogLayout, null) else dialogLayout as View

    if (dialogText != null)
        dialogView.findViewById<TextView>(R.id.custom_text).text =
            if (dialogText is Int) getString(dialogText) else dialogText as CharSequence

    return AlertDialog.Builder(this).apply {
        setCustomTitle(titleTextView)
        setView(dialogView)
        setCancelable(false)
        setPositiveButton(getString(positiveButtonLabel)) { _, _ ->
            positiveButtonFunctionality?.invoke()
        }
        setNegativeButton(negativeButtonLabel?.let { getString(it) }) { _, _ ->
            negativeButtonFunctionality?.invoke()
        }
    }
}

fun FileManagerActivity.noItemsSelectedDialog() {
    alertDialogBuilder(
        titleText = R.string.no_items_selected,
        dialogText = R.string.to_select_items
    ).show()
}

fun FileManagerActivity.alreadyExistsDialog(name: String) {
    alertDialogBuilder(
        titleText = name,
        dialogText = R.string.already_exist
    ).show()
}

fun FileManagerActivity.nameIsNullDialog() {
    alertDialogBuilder(
        titleText = R.string.empty_name,
        dialogText = R.string.enter_name
    ).show()
}

fun FileManagerActivity.copyOrMoveIntoItselfDialog(copyOrMove: String) {
    val text = when (copyOrMove) {
        "copy" -> R.string.cannot_copy_into_itself
        "move" -> R.string.cannot_move_into_itself
        else -> copyOrMove
    }

    alertDialogBuilder(
        titleText = getString(R.string.attention),
        dialogText = text
    ).show()
}

fun FileManagerActivity.noResultsDialog() {
    alertDialogBuilder(
        titleText = R.string.info,
        dialogText = R.string.no_results
    ).show()
}

fun FileManagerActivity.multiThreadedOperationsDialog() {
    alertDialogBuilder(
        titleText = R.string.info,
        dialogText = getString(R.string.multi_thread_op_not_impl)
    ).show()
}