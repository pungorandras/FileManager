package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import android.content.DialogInterface
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.mackhartley.roundedprogressbar.ProgressTextFormatter
import com.mackhartley.roundedprogressbar.RoundedProgressBar
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.R
import hu.pungor.filemanager.model.AboutFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

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

fun FileManagerActivity.progressBarBuilder(titleText: Int): RoundedProgressBar {
    val progressBar = findViewById<RoundedProgressBar>(R.id.progressBar).also {
        setProgressBarState(it, 0.0, false)
        setUpCustomProgressText(it, titleText)
    }
    setProgressLayoutVisibility(View.VISIBLE)

    return progressBar
}

private fun FileManagerActivity.setUpCustomProgressText(
    advancedBar: RoundedProgressBar,
    titleText: Int
) {
    val customFormatter = object : ProgressTextFormatter {
        override fun getProgressText(progressValue: Float): String {
            return getString(titleText) + " | " + (progressValue * 100).roundToInt()
                .toString() + "%"
        }
    }

    advancedBar.setProgressTextFormatter(customFormatter)
}

fun FileManagerActivity.setProgressLayoutVisibility(visibility: Int) {
    findViewById<RelativeLayout>(R.id.progressbar_layout).visibility = visibility
}

fun setProgressBarState(
    progressBar: RoundedProgressBar,
    progressPercentage: Double,
    animate: Boolean = true
) {
    CoroutineScope(Main).launch {
        progressBar.setProgressPercentage(
            progressPercentage = progressPercentage,
            shouldAnimate = animate
        )
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