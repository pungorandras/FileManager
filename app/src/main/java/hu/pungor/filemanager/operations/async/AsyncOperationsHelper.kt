package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import android.content.DialogInterface
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.widget.TextView
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.model.AboutFile
import java.io.File

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
        setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ ->
            buttonFunctionality.invoke()
        }
    }
}

fun getFolderSize(folder: File): Long {
    return folder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
}

fun getSelectedListSize(selectedList: List<AboutFile>): Double {
    var selectedListSize = 0.0
    selectedList.forEach {
        selectedListSize += if (it.mimeType == TYPE_FOLDER)
            getFolderSize(File(it.path))
        else
            File(it.path).length()
    }

    return selectedListSize
}