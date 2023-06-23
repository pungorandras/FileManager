package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import android.content.DialogInterface
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.model.AboutFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

fun FileManagerActivity.progressBarBuilder(titleText: Int): ProgressBar {
    val progressBar = findViewById<ProgressBar>(R.id.progressBar)
    findViewById<TextView>(R.id.progressText).text = getText(titleText)
    setProgressLayoutVisibility(View.VISIBLE)

    return progressBar
}

fun FileManagerActivity.setProgressLayoutVisibility(visibility: Int) {
    findViewById<RelativeLayout>(R.id.progressbar_layout).visibility = visibility
}

fun FileManagerActivity.setProgressBarState(
    progressBar: ProgressBar,
    progressPercentage: Double,
) {
    CoroutineScope(Main).launch {
        progressBar.progress = progressPercentage.toInt()
        val percentageString = progressBar.progress.toString() + "%"
        findViewById<TextView>(R.id.percentage).text = percentageString
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

fun cancelProgress(job: Job) {
    job.cancel()
}