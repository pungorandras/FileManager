package hu.pungor.filemanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import hu.pungor.filemanager.adapter.FileManagerAdapter
import hu.pungor.filemanager.intro.loadIntroScreen
import hu.pungor.filemanager.intro.loadTutorial
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.*
import hu.pungor.filemanager.permissions.activityResult
import hu.pungor.filemanager.permissions.checkPermissionsAndLoadFiles
import kotlinx.android.synthetic.main.activity_filemanager.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions
import java.io.File


@Suppress("DEPRECATION")
@RuntimePermissions
class FileManagerActivity : AppCompatActivity(), FileManagerAdapter.FileItemClickListener {

    val fmAdapter = FileManagerAdapter()

    var rootPath = File(Environment.getExternalStorageDirectory().absolutePath)
    var sdCardPath: File? = null
    var currentPath = rootPath

    companion object {
        const val TYPE_FOLDER = "folder"
        const val TYPE_UNKNOWN = "unknown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filemanager)

        checkPermissionsAndLoadFiles()
        disableSDCardButtonIfNotAvailable()
        loadIntroScreen()
        loadTutorial()

        Internal.setOnClickListener {
            internalButtonOperations()
        }

        SDCard.setOnClickListener {
            sdCardButtonOperations()
        }

        create_textfile.setOnClickListener {
            createTextFileBuilder()
        }

        create_folder.setOnClickListener {
            createFolderBuilder()
        }

        select_all.setOnClickListener {
            selectAllOperation()
        }

        delete_selected.setOnClickListener {
            deleteSelectedBuilder()
        }

        copy_selected.setOnClickListener {
            copySelectedOperation()
        }

        move_selected.setOnClickListener {
            moveSelectedOperation()
        }

        search.setOnClickListener {
            searchButtonOperations()
        }
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun loadFiles() {
        try {
            rvFiles.layoutManager = LinearLayoutManager(this)
            rvFiles.adapter = fmAdapter
            fmAdapter.setFiles(AsyncGetAllFiles().execute(this).get())
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
        showRationaleForStoragePermissionsBuilder(request)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000)
            loadFiles()
        if (requestCode == 1001)
            activityResult(requestCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentPathString = currentPath.toString()

        if (currentPath != rootPath && currentPath != sdCardPath && !fmAdapter.btnSearchPressed) {
            val location = currentPathString.substring(0, currentPathString.lastIndexOf("/") + 1)
            currentPath = File(location)
            loadFiles()
        } else if (fmAdapter.btnSearchPressed && fileTreeDepth > 0) {
            fileTreeDepth--

            if (fileTreeDepth == 0)
                fmAdapter.setFiles(fillList(result))
            else {
                val location =
                    currentPathString.substring(0, currentPathString.lastIndexOf("/") + 1)
                currentPath = File(location)
                loadFiles()
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
    override fun onItemLongClick(position: Int, view: View): Boolean {
        if (!fmAdapter.btnSearchPressed && !fmAdapter.btnCopyPressed && !fmAdapter.btnMovePressed) {
            val wrapper = ContextThemeWrapper(this, R.style.NoPopupAnimation)
            val popup = PopupMenu(wrapper, view, Gravity.END)
            popup.inflate(R.menu.menu_options)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.open_with -> {
                        openUnknown(fmAdapter.getItem(position))
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
                        deleteSelectedBuilder()
                        true
                    }
                    else -> false
                }
            }

            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popup)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
                Log.e("Main", "Error showing menu icons.", e)
            } finally {
                popup.show()
            }
            return false
        }
        return false
    }
}