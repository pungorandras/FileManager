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
import android.webkit.MimeTypeMap
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import hu.pungor.filemanager.adapter.FileManagerAdapter
import hu.pungor.filemanager.intro.IntroScreenActivity
import hu.pungor.filemanager.intro.TapTargetPromptInstructions
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.AsyncGetAllFiles
import hu.pungor.filemanager.operations.ButtonClickOperations
import hu.pungor.filemanager.operations.FileOperations
import hu.pungor.filemanager.permissions.SDCardPermissionsUntilApi29
import hu.pungor.filemanager.permissions.StoragePermissions
import kotlinx.android.synthetic.main.activity_filemanager.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.util.*


@Suppress("DEPRECATION")
@RuntimePermissions
class FileManagerActivity : AppCompatActivity(), FileManagerAdapter.FileItemClickListener {

    val fileManagerAdapter = FileManagerAdapter()
    private val buttonClickOperations = ButtonClickOperations()
    private val fileOperations = FileOperations()
    private val tapTargetPromptInstructions = TapTargetPromptInstructions()
    private val storagePermissions = StoragePermissions()
    private val sdCardPermissions = SDCardPermissionsUntilApi29()

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

        storagePermissions.checkPermissionsAndLoadFiles(this)
        buttonClickOperations.disableSDCardButtonIfNotAvailable(this)
        loadIntroScreen()
        loadTutorial()

        Internal.setOnClickListener {
            buttonClickOperations.internalButtonOperations(this)
        }

        SDCard.setOnClickListener {
            buttonClickOperations.sdCardButtonOperations(this)
        }

        create_textfile.setOnClickListener {
            buttonClickOperations.createTextFileBuilder(this)
        }

        create_folder.setOnClickListener {
            buttonClickOperations.createFolderBuilder(this)
        }

        select_all.setOnClickListener {
            buttonClickOperations.selectAllOperation(this)
        }

        delete_selected.setOnClickListener {
            buttonClickOperations.deleteSelectedBuilder(this)
        }

        copy_selected.setOnClickListener {
            buttonClickOperations.copySelectedOperation(this)
        }

        move_selected.setOnClickListener {
            buttonClickOperations.moveSelectedOperation(this)
        }

        search.setOnClickListener {
            buttonClickOperations.searchButtonOperations(this)
        }
    }

    private fun loadIntroScreen() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val firstStart = prefs.getBoolean("firstStart", true)

        if (firstStart)
            startActivity(Intent(this, IntroScreenActivity::class.java))

        val editor = prefs.edit()
        editor.putBoolean("firstStart", false)
        editor.apply()
    }

    private fun loadTutorial() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val firstStart = prefs.getBoolean("tutorial", true)

        if (firstStart)
            tapTargetPromptInstructions.showTutorial(this)

        val editor = prefs.edit()
        editor.putBoolean("tutorial", false)
        editor.apply()
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun loadFiles() {
        try {
            rvFiles.layoutManager = LinearLayoutManager(this)
            rvFiles.adapter = fileManagerAdapter
            fileManagerAdapter.setFiles(AsyncGetAllFiles().execute(this).get())
            fileManagerAdapter.itemClickListener = this
        } catch (e: Exception) {
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
        buttonClickOperations.showRationaleForStoragePermissionsBuilder(request, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sdCardPermissions.activityResult(requestCode, data, this)
    }

    fun fillList(fileList: List<File>): List<AboutFile> {
        val mutableFileList = mutableListOf<AboutFile>()

        if (fileList.isNotEmpty()) {
            for (currentFile in fileList) {
                val uri = currentFile.path
                val uriWithoutPrefix = uri.removePrefix("/storage/emulated/0/")
                val extension = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

                val name =
                    if (fileManagerAdapter.btnSearchPressed) uriWithoutPrefix else currentFile.name

                if (currentFile.isDirectory)
                    mutableFileList += AboutFile(name, "", uri, TYPE_FOLDER, false)
                else if (mimeType.isNullOrEmpty())
                    mutableFileList += AboutFile(
                        name,
                        getSize(currentFile),
                        uri,
                        TYPE_UNKNOWN,
                        false
                    )
                else
                    mutableFileList += AboutFile(
                        name,
                        getSize(currentFile),
                        uri,
                        mimeType.toString(),
                        false
                    )
            }
        }
        return sortList(mutableFileList)
    }

    private fun getSize(file: File): String {
        val GB: Long = 1024 * 1024 * 1024
        val MB: Long = 1024 * 1024
        val kB: Long = 1024
        val size_in_bytes = file.length().toDouble()

        if (size_in_bytes > GB)
            return String.format("%.1f", size_in_bytes / GB) + "\u00A0GB"
        else if (size_in_bytes > MB)
            return String.format("%.1f", size_in_bytes / MB) + "\u00A0MB"
        else if (size_in_bytes > kB)
            return String.format("%.1f", size_in_bytes / kB) + "\u00A0kB"
        else
            return String.format("%.1f", size_in_bytes) + "\u00A0B"
    }

    private fun sortList(list: List<AboutFile>): List<AboutFile> {
        val folderList = mutableListOf<AboutFile>()
        val fileList = mutableListOf<AboutFile>()

        for (file in list) {
            if (file.mimeType == TYPE_FOLDER)
                folderList.add(file)
            else
                fileList.add(file)
        }

        folderList.sortBy { it.name.toLowerCase(Locale.ROOT) }
        fileList.sortBy { it.name.toLowerCase(Locale.ROOT) }

        return folderList + fileList
    }

    override fun onBackPressed() {
        val currentPathString = currentPath.toString()

        if (currentPath != rootPath && currentPath != sdCardPath && !fileManagerAdapter.btnSearchPressed) {
            val location = currentPathString.substring(0, currentPathString.lastIndexOf("/") + 1)
            currentPath = File(location)

            loadFiles()
        } else if (fileManagerAdapter.btnSearchPressed && buttonClickOperations.fileTreeDepth > 0) {
            buttonClickOperations.fileTreeDepth--

            if (buttonClickOperations.fileTreeDepth == 0)
                fileManagerAdapter.setFiles(fillList(buttonClickOperations.result))
            else {
                val location =
                    currentPathString.substring(0, currentPathString.lastIndexOf("/") + 1)
                currentPath = File(location)

                loadFiles()
            }
        }
    }

    override fun onItemClick(file: AboutFile) {
        if (file.mimeType == TYPE_FOLDER)
            fileOperations.openFolder(file, this, buttonClickOperations)
        else if (file.mimeType == TYPE_UNKNOWN)
            fileOperations.openUnknown(file, this)
        else
            fileOperations.openFile(file, this)
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onItemLongClick(position: Int, view: View): Boolean {
        if (!fileManagerAdapter.btnSearchPressed && !fileManagerAdapter.btnCopyPressed && !fileManagerAdapter.btnMovePressed) {
            val wrapper = ContextThemeWrapper(this, R.style.NoPopupAnimation)
            val popup = PopupMenu(wrapper, view, Gravity.END)
            popup.inflate(R.menu.menu_options)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.open_with -> {
                        fileOperations.openUnknown(fileManagerAdapter.getItem(position), this)
                        true
                    }
                    R.id.share -> {
                        fileOperations.shareFile(view, position, this)
                        true
                    }
                    R.id.copy -> {
                        fileManagerAdapter.popupMenuPressed = true
                        fileManagerAdapter.backupSelectedList()
                        fileManagerAdapter.clearSelectedList()
                        fileManagerAdapter.addToSelectedList(fileManagerAdapter.getItem(position))
                        buttonClickOperations.copySelectedOperation(this)
                        true
                    }
                    R.id.move -> {
                        fileManagerAdapter.popupMenuPressed = true
                        fileManagerAdapter.backupSelectedList()
                        fileManagerAdapter.clearSelectedList()
                        fileManagerAdapter.addToSelectedList(fileManagerAdapter.getItem(position))
                        buttonClickOperations.moveSelectedOperation(this)
                        true
                    }
                    R.id.rename -> {
                        fileOperations.renameFile(view, position, this)
                        true
                    }
                    R.id.delete -> {
                        fileManagerAdapter.popupMenuPressed = true
                        fileManagerAdapter.backupSelectedList()
                        fileManagerAdapter.clearSelectedList()
                        fileManagerAdapter.addToSelectedList(fileManagerAdapter.getItem(position))
                        buttonClickOperations.deleteSelectedBuilder(this)
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