package hu.pungor.filemanager.operations.async

import android.app.ProgressDialog
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.File

private lateinit var searchResult: Deferred<MutableList<File>>

@Suppress("DEPRECATION")
suspend fun FileManagerActivity.asyncSearch(input: String): MutableList<File> {

    val sBuilder = SpannableStringBuilder(getString(R.string.wait)).apply {
        val spanResource = resources.getDimensionPixelSize(R.dimen.wait)
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        setSpan(AbsoluteSizeSpan(spanResource), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    val progressDialog = progressDialogBuilder(
        titleText = R.string.searching,
        message = sBuilder,
        progressStyle = ProgressDialog.STYLE_SPINNER,
        buttonFunctionality = { searchResult.cancel() }
    ).apply { show() }

    searchResult = CoroutineScope(IO).async {
        val result = mutableListOf<File>()
        currentPath.walk().takeWhile { isActive }.forEach {
            if (it.name.lowercase().contains(input.lowercase()) && it.path != currentPath.path)
                result += it
            if (!isActive)
                return@forEach
        }
        progressDialog.dismiss()
        return@async result
    }
    return searchResult.await()
}