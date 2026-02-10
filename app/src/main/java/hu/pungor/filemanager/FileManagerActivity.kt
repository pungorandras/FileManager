package hu.pungor.filemanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import hu.pungor.filemanager.adapter.FileManagerAdapter
import hu.pungor.filemanager.alertdialog.multiThreadedOperationsDialog
import hu.pungor.filemanager.databinding.ActivityFilemanagerBinding
import hu.pungor.filemanager.databinding.BottomButtonsLayoutBinding
import hu.pungor.filemanager.databinding.FilemanagerRecyclerviewBinding
import hu.pungor.filemanager.databinding.ProgressbarLayoutBinding
import hu.pungor.filemanager.databinding.TopButtonsLayoutBinding
import hu.pungor.filemanager.intro.loadIntroScreen
import hu.pungor.filemanager.intro.loadTutorial
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.async.cancelProgress
import hu.pungor.filemanager.operations.async.isSearchResultInitialized
import hu.pungor.filemanager.operations.async.listFiles
import hu.pungor.filemanager.operations.async.resetProgressBar
import hu.pungor.filemanager.operations.async.searchResult
import hu.pungor.filemanager.operations.async.setProgressLayoutVisibility
import hu.pungor.filemanager.operations.async.somethingInProgress
import hu.pungor.filemanager.operations.copySelectedOperation
import hu.pungor.filemanager.operations.createFolderDialog
import hu.pungor.filemanager.operations.createTextFileDialog
import hu.pungor.filemanager.operations.deleteSelectedDialog
import hu.pungor.filemanager.operations.disableSDCardButtonIfNotAvailable
import hu.pungor.filemanager.operations.fileTreeDepth
import hu.pungor.filemanager.operations.internalButtonOperations
import hu.pungor.filemanager.operations.moveSelectedOperation
import hu.pungor.filemanager.operations.openFile
import hu.pungor.filemanager.operations.openFolder
import hu.pungor.filemanager.operations.openUnknown
import hu.pungor.filemanager.operations.renameFile
import hu.pungor.filemanager.operations.result
import hu.pungor.filemanager.operations.sdCardButtonOperations
import hu.pungor.filemanager.operations.searchButtonOperations
import hu.pungor.filemanager.operations.selectAllOperation
import hu.pungor.filemanager.operations.shareFile
import hu.pungor.filemanager.operations.showRationaleForStoragePermissionsDialog
import hu.pungor.filemanager.permissions.checkPermissionsAndLoadFiles
import hu.pungor.filemanager.permissions.grantRWPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions
import java.io.File


@RuntimePermissions
class FileManagerActivity : AppCompatActivity(), FileManagerAdapter.FileItemClickListener {

    private lateinit var recyclerBinding: FilemanagerRecyclerviewBinding
    private lateinit var progressBinding: ProgressbarLayoutBinding
    lateinit var topBinding: TopButtonsLayoutBinding
    lateinit var bottomBinding: BottomButtonsLayoutBinding

    val vcIsR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val fmAdapter = FileManagerAdapter()
    var rootPath = File(Environment.getExternalStorageDirectory().absolutePath)
    var sdCardPath: File? = null
    var currentPath = rootPath

    lateinit var progressBar: ProgressBar
    lateinit var job: Job
    var selectedListSize = 0.0
    var progressState = 0.0

    fun isJobInitialized() = ::job.isInitialized

    companion object {
        const val TYPE_FOLDER = "folder"
        const val TYPE_UNKNOWN = "unknown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityFilemanagerBinding.inflate(layoutInflater).root)
        recyclerBinding = FilemanagerRecyclerviewBinding.bind(findViewById(R.id.rvFiles))
        topBinding = TopButtonsLayoutBinding.bind(findViewById(R.id.top_buttons_layout))
        bottomBinding = BottomButtonsLayoutBinding.bind(findViewById(R.id.bottom_button_layout))
        progressBinding = ProgressbarLayoutBinding.bind(findViewById(R.id.progressbar_layout))

        CoroutineScope(Main).launch { loadIntroScreen() }
        checkPermissionsAndLoadFiles()
        disableSDCardButtonIfNotAvailable()
        loadTutorial()

        topBinding.Internal.setOnClickListener {
            internalButtonOperations()
        }

        topBinding.SDCard.setOnClickListener {
            sdCardButtonOperations()
        }

        bottomBinding.createTextfile.setOnClickListener {
            createTextFileDialog()
        }

        bottomBinding.createFolder.setOnClickListener {
            createFolderDialog()
        }

        bottomBinding.selectAll.setOnClickListener {
            selectAllOperation()
        }

        bottomBinding.deleteSelected.setOnClickListener {
            if (!somethingInProgress())
                deleteSelectedDialog()
            else
                multiThreadedOperationsDialog()
        }

        bottomBinding.copySelected.setOnClickListener {
            if (!somethingInProgress())
                copySelectedOperation()
            else
                multiThreadedOperationsDialog()
        }

        bottomBinding.moveSelected.setOnClickListener {
            if (!somethingInProgress())
                moveSelectedOperation()
            else
                multiThreadedOperationsDialog()
        }

        bottomBinding.search.setOnClickListener {
            if (!somethingInProgress())
                searchButtonOperations()
            else
                multiThreadedOperationsDialog()
        }

        progressBinding.cancelProgress.setOnClickListener {
            if (isJobInitialized())
                cancelProgress(job)
            if (isSearchResultInitialized()) {
                searchResult.cancel()
                setProgressLayoutVisibility(View.GONE)
                resetProgressBar()
            }
        }
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun loadFiles() {
        try {
            recyclerBinding.rvFiles.layoutManager = LinearLayoutManager(this)
            recyclerBinding.rvFiles.adapter = fmAdapter
            listFiles()
            fmAdapter.itemClickListener = this
        } catch (e: Exception) {
            Log.e("Main", "Error loading files.", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @OnShowRationale(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun showRationaleForStoragePermissions(request: PermissionRequest) {
        showRationaleForStoragePermissionsDialog(request)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000)
            loadFiles()
        if (requestCode == 1001)
            grantRWPermissions(data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentPathString = currentPath.toString()

        if (currentPath != rootPath && currentPath != sdCardPath && !fmAdapter.btnSearchPressed) {
            val location = currentPathString.substring(0, currentPathString.lastIndexOf("/") + 1)
            currentPath = File(location)
            listFiles()
        } else if (fmAdapter.btnSearchPressed && fileTreeDepth > 0) {
            fileTreeDepth--

            if (fileTreeDepth == 0)
                listFiles(result)
            else {
                val location =
                    currentPathString.substring(0, currentPathString.lastIndexOf("/") + 1)
                currentPath = File(location)
                listFiles()
            }
        }
    }

    override fun onItemClick(file: AboutFile) {
        when (file.mimeType) {
            TYPE_FOLDER -> openFolder(file)
            TYPE_UNKNOWN -> openUnknown(file)
            else -> openFile(file)
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onItemLongClick(position: Int, view: View) {
        if (!fmAdapter.btnSearchPressed && !fmAdapter.btnCopyPressed && !fmAdapter.btnMovePressed) {
            val wrapper = ContextThemeWrapper(this, R.style.NoPopupAnimation)
            val popup = PopupMenu(wrapper, view, Gravity.END).apply { inflate(R.menu.menu_options) }
            val currentItem = fmAdapter.getItem(position)

            if (currentItem.mimeType == TYPE_FOLDER) {
                val menu = popup.menu
                menu.findItem(R.id.open_with).isVisible = false
                menu.findItem(R.id.share).isVisible = false
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.open_with -> {
                        openUnknown(currentItem)
                        true
                    }

                    R.id.share -> {
                        shareFile(view, position)
                        true
                    }

                    R.id.copy -> {
                        fmAdapter.popupMenuPressActions(position)
                        copySelectedOperation()
                        true
                    }

                    R.id.move -> {
                        fmAdapter.popupMenuPressActions(position)
                        moveSelectedOperation()
                        true
                    }

                    R.id.rename -> {
                        renameFile(view, position)
                        true
                    }

                    R.id.delete -> {
                        fmAdapter.popupMenuPressActions(position)
                        deleteSelectedDialog()
                        true
                    }

                    else -> false
                }
            }

            try {
                PopupMenu::class.java.getDeclaredField("mPopup").apply {
                    isAccessible = true
                }.get(popup).apply {
                    javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                        .invoke(this, true)
                }
            } catch (e: Exception) {
                Log.e("Main", "Error showing menu icons.", e)
            } finally {
                popup.show()
            }
        }
    }
}