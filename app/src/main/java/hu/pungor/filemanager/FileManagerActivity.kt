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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import hu.pungor.filemanager.adapter.FileManagerAdapter
import hu.pungor.filemanager.model.AboutFile
import hu.pungor.filemanager.operations.AsyncGetAllFiles
import hu.pungor.filemanager.operations.ButtonClickOperations
import hu.pungor.filemanager.operations.FileOperations
import kotlinx.android.synthetic.main.activity_filemanager.*
import permissions.dispatcher.*
import java.io.File
import java.util.*


@Suppress("DEPRECATION")
@RuntimePermissions
class FileManagerActivity : AppCompatActivity(), FileManagerAdapter.FileItemClickListener {

    val fileManagerAdapter = FileManagerAdapter()
    private val buttonClickOperations = ButtonClickOperations(this)
    private val fileOperations = FileOperations(this)

    var rootPath = File(Environment.getExternalStorageDirectory().absolutePath)
    var sdCardPath: File? = getSDCardPath()
    var currentPath = rootPath

    companion object {
        const val TYPE_FOLDER = "folder"
        const val TYPE_UNKNOWN = "unknown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filemanager)

        loadFilesWithPermissionCheck()

        if (sdCardPath == null) {
            SDCard.isEnabled = false
            SDCard.backgroundTintList =
                applicationContext.resources.getColorStateList(R.color.disabled)
            SDCard.text = getString(R.string.no_sdcard)
        }

        Internal.setOnClickListener {
                currentPath = rootPath
                loadFilesWithPermissionCheck()
        }

        SDCard.setOnClickListener {
            sdCardPath = getSDCardPath()
            sdCardPermissions(sdCardPath!!)
            currentPath = sdCardPath!!
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
            Toast.makeText(applicationContext, getString(R.string.doesnt_exist), Toast.LENGTH_SHORT).show()
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

    @OnPermissionDenied(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onPermissionDenied() {
        Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
    }

    fun fillList(fileList: List<File>): List<AboutFile> {
        val mutableFileList = mutableListOf<AboutFile>()

        if (fileList.isNotEmpty()) {
            for (currentFile in fileList) {
                val uri = currentFile.path
                val uriWithoutPrefix = uri.removePrefix("/storage/emulated/0/")
                val extension = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

                val name = if(fileManagerAdapter.btnSearchPressed) uriWithoutPrefix else currentFile.name

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

        if (storage != null) {
            for (element in storage) {
                if (element.name != "emulated" && element.name != "self")
                    sdCardPath = element
            }
        }

        return sdCardPath
    }

    private fun sdCardPermissions(sdCardRootPath: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
                val storageVolume = storageManager.getStorageVolume(sdCardRootPath)
                val intent = storageVolume?.createAccessIntent(null)
                startActivityForResult(intent, 1000)
            } catch (e: Exception) {}
        }

        if(Build.VERSION.SDK_INT < 24){
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1001)
            } catch (e: Exception) {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data!!)
            if (requestCode == 1000 || requestCode ==1001) {
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
            Toast.makeText(applicationContext, getString(R.string.sd_permission_denied), Toast.LENGTH_SHORT)
                .show()
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
        if (!fileManagerAdapter.btnSearchPressed && !fileManagerAdapter.popupMenuButtonPressed && !fileManagerAdapter.btnCopyPressed && !fileManagerAdapter.btnMovePressed) {
            val wrapper = ContextThemeWrapper(this, R.style.NoPopupAnimation)
            val popup = PopupMenu(wrapper, view, Gravity.END)
            popup.inflate(R.menu.menu_options)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.share -> {
                        fileOperations.shareFile(view, position, this)
                        true
                    }
                    R.id.copy -> {
                        fileManagerAdapter.backupSelectedList()
                        fileManagerAdapter.popupMenuButtonPressed = true
                        fileManagerAdapter.clearSelectedList()
                        fileManagerAdapter.addToSelectedList(fileManagerAdapter.getItem(position))
                        copy_selected.performClick()
                        true
                    }
                    R.id.move -> {
                        fileManagerAdapter.backupSelectedList()
                        fileManagerAdapter.popupMenuButtonPressed = true
                        fileManagerAdapter.clearSelectedList()
                        fileManagerAdapter.addToSelectedList(fileManagerAdapter.getItem(position))
                        move_selected.performClick()
                        true
                    }
                    R.id.rename -> {
                        fileOperations.renameFile(view, position, this)
                        true
                    }
                    R.id.delete -> {
                        fileManagerAdapter.backupSelectedList()
                        fileManagerAdapter.popupMenuButtonPressed = true
                        fileManagerAdapter.clearSelectedList()
                        fileManagerAdapter.addToSelectedList(fileManagerAdapter.getItem(position))
                        delete_selected.performClick()
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