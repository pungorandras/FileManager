package hu.pungor.filemanager.alertdialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R

private fun FileManagerActivity.showDialog(customTitle: View, customText: View) {
    val builder = AlertDialog.Builder(this)
        .setCustomTitle(customTitle)
        .setView(customText)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.ok), null)
    builder.show()
}

@SuppressLint("InflateParams")
private fun FileManagerActivity.inflateDialog(titleText: String, dialogText: String) {
    val customTitle = LayoutInflater.from(this).inflate(R.layout.custom_title, null)
    customTitle.findViewById<TextView>(R.id.title_text).text = titleText

    val customText = LayoutInflater.from(this).inflate(R.layout.custom_text_alertdialog, null)
    customText.findViewById<TextView>(R.id.custom_text).text = dialogText

    showDialog(customTitle, customText)
}

fun FileManagerActivity.noItemsSelectedDialog() {
    inflateDialog(
        titleText = getString(R.string.no_items_selected),
        dialogText = getString(R.string.to_select_items)
    )
}

fun FileManagerActivity.alreadyExistsDialog(name: String) {
    inflateDialog(
        titleText = name,
        dialogText = getString(R.string.already_exist)
    )
}

fun FileManagerActivity.nameIsNullDialog() {
    inflateDialog(
        titleText = getString(R.string.empty_name),
        dialogText = getString(R.string.enter_name)
    )
}

@SuppressLint("InflateParams")
fun FileManagerActivity.copyOrMoveIntoItselfDialog(copyOrMove: String) {
    val text = when (copyOrMove) {
        "copy" -> getString(R.string.cannot_copy_into_itself)
        "move" -> getString(R.string.cannot_move_into_itself)
        else -> ""
    }

    inflateDialog(
        titleText = getString(R.string.attention),
        dialogText = text
    )
}

@SuppressLint("InflateParams")
fun FileManagerActivity.noResultsDialog() {
    inflateDialog(
        titleText = getString(R.string.info),
        dialogText = getString(R.string.no_results)
    )
}
