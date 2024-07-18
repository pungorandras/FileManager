package hu.pungor.filemanager.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_FOLDER
import hu.pungor.filemanager.FileManagerActivity.Companion.TYPE_UNKNOWN
import hu.pungor.filemanager.R
import hu.pungor.filemanager.databinding.ItemFileBinding
import hu.pungor.filemanager.model.AboutFile

class FileManagerAdapter : RecyclerView.Adapter<FileManagerAdapter.FileManagerViewHolder>() {

    private val fileList = mutableListOf<AboutFile>()
    private var selectedList = mutableListOf<AboutFile>()
    private var selectedListBackup = mutableListOf<AboutFile>()

    var clearSelectedList = true
    var btnSelectAllPressed = false
    var btnCopyPressed = false
    var btnMovePressed = false
    var btnSearchPressed = false
    var popupMenuPressed = false

    var itemClickListener: FileItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileManagerViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileManagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileManagerViewHolder, position: Int) {
        val file = fileList[position]
        holder.bind(file)

        Glide.with(holder.binding.fileIcon.context).clear(holder.binding.fileIcon)
        setDrawableOnLoad(holder, position)

        holder.binding.fileIcon.setOnClickListener {
            if (!btnCopyPressed && !btnMovePressed && !btnSearchPressed) {
                if (file.selected) {
                    file.selected = false
                    removeFromSelectedList(file)
                    setDrawableOnLoad(holder, position)
                } else if (holder.binding.fileIcon.drawable != null) {
                    file.selected = true
                    addToSelectedList(file)
                    val layerDrawable = tickOverlay(holder, holder.binding.fileIcon.drawable)
                    Glide.with(holder.itemView).load(layerDrawable)
                        .diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.binding.fileIcon)
                }
            }
        }
    }

    @SuppressLint("IntentReset")
    private fun setDrawableOnLoad(holder: FileManagerViewHolder, position: Int) {
        val file = fileList[position]
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(file.path)
            type = file.mimeType
        }
        val matches =
            holder.binding.fileIcon.context.packageManager.queryIntentActivities(intent, 0)

        val resource: Any = when {
            file.mimeType == TYPE_FOLDER -> R.drawable.folder
            file.mimeType == TYPE_UNKNOWN || matches.isNullOrEmpty() -> R.drawable.file_icon_default
            mediaFile(file) -> file.path
            else -> matches[0].loadIcon(holder.binding.fileIcon.context.packageManager)
        }

        configureGlide(holder, resource, file)
    }

    private fun configureGlide(holder: FileManagerViewHolder, resource: Any, file: AboutFile) {
        var obj = Glide.with(holder.itemView).load(resource).override(100, 100)
            .diskCacheStrategy(DiskCacheStrategy.ALL).listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (file.selected && !popupMenuPressed) {
                        val layerDrawable = tickOverlay(holder, resource)
                        holder.binding.fileIcon.setImageDrawable(layerDrawable)
                        return true
                    }
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (file.selected && !popupMenuPressed && mediaFile(file)) {
                        val context = holder.binding.fileIcon.context
                        val drawable = ContextCompat.getDrawable(context, R.drawable.checkmark)
                        val layerDrawable = tickOverlay(holder, drawable)
                        holder.binding.fileIcon.setImageDrawable(layerDrawable)
                        return true
                    }
                    return false
                }
            })

        if (mediaFile(file))
            obj = obj.apply(RequestOptions().centerCrop()).placeholder(R.drawable.file_icon_default)

        obj.into(holder.binding.fileIcon)
    }

    private fun mediaFile(file: AboutFile): Boolean {
        return file.mimeType.startsWith("image") || file.mimeType.startsWith("video")
    }

    private fun tickOverlay(holder: FileManagerViewHolder, drawable: Drawable?): LayerDrawable {
        val context = holder.binding.fileIcon.context
        val layers = arrayOfNulls<Drawable>(2)
        layers[0] = drawable
        layers[0]?.alpha = 50
        layers[1] = ContextCompat.getDrawable(context, R.drawable.checkmark)
        return LayerDrawable(layers)
    }

    private fun setSelectedOnLoad(files: List<AboutFile>): List<AboutFile> {
        files.forEach { file ->
            if (selectedList.any { it.path == file.path }) {
                file.selected = true
            }
        }
        return files
    }

    private fun removeFromSelectedList(file: AboutFile) {
        selectedList.removeAll { it.path == file.path }
    }

    fun clearSelectedList() {
        selectedList.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFiles(files: List<AboutFile>) {
        fileList.clear()

        if (clearSelectedList) {
            clearSelectedList()
            btnSelectAllPressed = false
        }
        fileList += setSelectedOnLoad(files)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    fun getItem(position: Int): AboutFile {
        return fileList[position]
    }

    fun getSelectedList(): List<AboutFile> {
        return selectedList
    }

    private fun backupSelectedList() {
        selectedListBackup = selectedList.toMutableList()
    }

    fun restoreSelectedList() {
        selectedList = selectedListBackup.toMutableList()
    }

    private fun addToSelectedList(file: AboutFile) {
        selectedList.add(file)
    }

    fun addAllToSelectedList() {
        fileList.forEach { if (!selectedList.contains(it)) addToSelectedList(it) }
    }

    fun popupMenuPressActions(position: Int) {
        popupMenuPressed = true
        backupSelectedList()
        clearSelectedList()
        addToSelectedList(getItem(position))
    }

    inner class FileManagerViewHolder(val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: AboutFile) {
            binding.fileName.text = file.name
            binding.fileInfo.text = file.info
            this.file = file
        }

        var file: AboutFile? = null

        init {
            itemView.setOnClickListener {
                file?.let { itemClickListener?.onItemClick(it) }
            }

            itemView.setOnLongClickListener { view ->
                itemClickListener?.onItemLongClick(adapterPosition, view)
                true
            }
        }
    }

    interface FileItemClickListener {
        fun onItemClick(file: AboutFile)
        fun onItemLongClick(position: Int, view: View)
    }
}