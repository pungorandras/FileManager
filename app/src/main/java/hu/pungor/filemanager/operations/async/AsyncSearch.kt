package hu.pungor.filemanager.operations.async

import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

lateinit var searchResult: Deferred<MutableList<File>>
fun isSearchResultInitialized() = ::searchResult.isInitialized

suspend fun FileManagerActivity.asyncSearch(input: String): MutableList<File> {

    searchResult = CoroutineScope(IO).async {
        withContext(Main) {
            progressBar = progressBarBuilder(R.string.searching).apply {
                isIndeterminate = true
            }
        }

        val result = mutableListOf<File>()
        currentPath.walk().takeWhile { isActive }.forEach {
            if (it.name.lowercase().contains(input.lowercase()) && it.path != currentPath.path)
                result += it
        }
        return@async result
    }
    return searchResult.await()
}