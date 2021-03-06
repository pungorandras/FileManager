package hu.pungor.filemanager

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import hu.pungor.filemanager.adapter.FileManagerAdapter
import hu.pungor.filemanager.alertdialog.AlertDialogMessages
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.AsyncGetAllFiles
import hu.pungor.filemanager.operations.ButtonClickOperations
import hu.pungor.filemanager.operations.FileOperations
import kotlinx.android.synthetic.main.activity_filemanager.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.util.*
import java.util.regex.Pattern


@Suppress("DEPRECATION")
@RuntimePermissions
class FileManagerActivity : AppCompatActivity(), FileManagerAdapter.FileItemClickListener {

    val fileManagerAdapter = FileManagerAdapter()
    private val buttonClickOperations = ButtonClickOperations(this)
    private val alertDialogMessages = AlertDialogMessages()
    private val fileOperations = FileOperations(this)

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

        loadFilesWithPermissionCheck()

        sdCardPathIsNull()

        Internal.setOnClickListener {
            currentPath = rootPath
            Internal.backgroundTintList =
                applicationContext.resources.getColorStateList(R.color.button_pressed)
            if (sdCardPath != null) {
                SDCard.backgroundTintList =
                    applicationContext.resources.getColorStateList(R.color.button)
            }

            loadFilesWithPermissionCheck()
        }

        SDCard.setOnClickListener {
            sdCardPath = getSDCardPath()

            if (!sdCardPath.toString().contains("null")) {
                Internal.backgroundTintList =
                    applicationContext.resources.getColorStateList(R.color.button)
                SDCard.backgroundTintList =
                    applicationContext.resources.getColorStateList(R.color.button_pressed)
                currentPath = sdCardPath!!
            } else
                sdCardPathIsNull()

            loadFilesWithPermissionCheck()
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

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val firstStart = prefs.getBoolean("firstStart", true)

        if (firstStart)
            alertDialogMessages.showOnFirstStart(this)
    }

    @OnShowRationale(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun showRationaleForStoragePermissions(request: PermissionRequest) {
        buttonClickOperations.showRationaleForStoragePermissionsBuilder(request, this)
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

    private fun getSDCardPath(): File? {
        val storage: Array<File>? = File("/storage").listFiles()
        var sdCardPath: File? = null
        val pattern = Pattern.compile("([A-Z]|[0-9]){4}-([A-Z]|[0-9]){4}")
        val regex = pattern.toRegex()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (storage != null) {
                for (element in storage) {
                    if (pattern.matcher(element.name).matches())
                        sdCardPath = element
                }
            }
            return sdCardPath
        } else {
            if (getUri() == null)
                buttonClickOperations.sdCardPermissionsBuilder(this)
            return File("/storage/" + getUri()?.path?.let { regex.find(it)?.value })
        }
    }

    private fun sdCardPathIsNull() {
        if (applicationContext.externalMediaDirs.size < 2)
            SDCard.visibility = View.GONE
    }

    private fun sdCardPermissions(sdCardRootPath: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
                val storageVolume = storageManager.getStorageVolume(sdCardRootPath)
                val intent = storageVolume?.createAccessIntent(null)
                startActivityForResult(intent, 1000)
            } catch (e: Exception) {
            }
        } else {
            try {
                buttonClickOperations.sdCardPermissionsBuilder(this)
            } catch (e: Exception) {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data!!)
            if (requestCode == 1000 || requestCode == 1001) {
                val uri = data.data
                grantUriPermission(
                    packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val takeFlags = data.flags and (Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION)
                contentResolver.takePersistableUriPermission(uri!!, takeFlags)
            }
        } catch (e: Exception) {
        }
    }

    fun getUri(): Uri? {
        try {
            val persistedUriPermissions =
                contentResolver.persistedUriPermissions
            if (persistedUriPermissions.size > 0) {
                val uriPermission = persistedUriPermissions[0]
                return uriPermission.uri
            }
        } catch (e: Exception) {
            sdCardPermissions(sdCardPath!!)
        }
        return null
    }

    override fun onBackPressed() {
        if (currentPath != rootPath && currentPath != sdCardPath && !fileManagerAdapter.btnSearchPressed) {
            val location =
                currentPath.toString().substring(0, currentPath.toString().lastIndexOf("/") + 1)
            currentPath = File(location)

            loadFiles()
        } else if (fileManagerAdapter.btnSearchPressed && buttonClickOperations.fileTreeDepth > 0) {
            buttonClickOperations.fileTreeDepth--

            if (buttonClickOperations.fileTreeDepth == 0)
                fileManagerAdapter.setFiles(fillList(buttonClickOperations.result))
            else {
                val location =
                    currentPath.toString().substring(0, currentPath.toString().lastIndexOf("/") + 1)
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