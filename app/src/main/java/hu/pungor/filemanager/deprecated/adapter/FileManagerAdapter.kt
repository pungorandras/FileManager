package hu.pungor.filemanager.deprecated.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import hu.pungor.filemanager.R
import hu.pungor.filemanager.deprecated.model.AboutFile
import kotlinx.android.synthetic.main.item_file.view.*


@Suppress("DEPRECATION")
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

    companion object {
        private const val TYPE_FOLDER = "folder"
        private const val TYPE_UNKNOWN = "unknown"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileManagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, null)
        return FileManagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileManagerViewHolder, position: Int) {
        val file = fileList[position]
        holder.file_name.text = file.name
        holder.file_info.text = file.info
        holder.file = file

        setDrawableOnLoad(holder, position)

        holder.file_icon.setOnClickListener {
            if (file.selected && !btnCopyPressed && !btnMovePressed && !btnSearchPressed) {
                file.selected = false
                removeFromSelectedList(file)
                setDrawableOnLoad(holder, position)
            } else if (holder.file_icon.drawable != null && !btnCopyPressed && !btnMovePressed && !btnSearchPressed) {
                file.selected = true
                selectedList.add(file)
                val layerDrawable = tickOverlay(holder, holder.file_icon.drawable)
                Glide.with(holder.itemView).load(layerDrawable)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.file_icon)
            }
        }
    }

    private fun setDrawableOnLoad(holder: FileManagerViewHolder, position: Int) {
        val file = fileList[position]
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(file.path)
        intent.type = file.mimeType
        val matches = holder.file_icon.context.packageManager.queryIntentActivities(intent, 0)

        if (file.mimeType == TYPE_FOLDER)
            Glide.with(holder.itemView).load(R.drawable.folder).override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (file.selected && !popupMenuPressed) {
                            val layerDrawable = tickOverlay(holder, resource)
                            holder.file_icon.setImageDrawable(layerDrawable)
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
                        return false
                    }
                }).into(holder.file_icon)
        else if (file.mimeType == TYPE_UNKNOWN || matches.isNullOrEmpty())
            Glide.with(holder.itemView).load(R.drawable.file_icon_default).override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (file.selected && !popupMenuPressed) {
                            val layerDrawable = tickOverlay(holder, resource)
                            holder.file_icon.setImageDrawable(layerDrawable)
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
                        return false
                    }
                }).into(holder.file_icon)
        else if (file.mimeType.startsWith("image") || file.mimeType.startsWith("video")) {
            val cropOptions = RequestOptions().centerCrop()
            Glide.with(holder.itemView).load(file.path).apply(cropOptions).override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.file_icon_default)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (file.selected && !popupMenuPressed) {
                            val layerDrawable = tickOverlay(holder, resource)
                            holder.file_icon.setImageDrawable(layerDrawable)
                            return true
                        }
                        return false
                    }

                    @SuppressLint("UseCompatLoadingForDrawables")
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (file.selected && !popupMenuPressed) {
                            val r = holder.file_icon.resources
                            val layerDrawable =
                                tickOverlay(holder, r.getDrawable(R.drawable.file_icon_default))
                            holder.file_icon.setImageDrawable(layerDrawable)
                            return true
                        }
                        return false
                    }
                }).into(holder.file_icon)

        } else
            Glide.with(holder.itemView)
                .load(matches[0].loadIcon(holder.file_icon.context.packageManager))
                .override(100, 100).diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (file.selected && !popupMenuPressed) {
                            val layerDrawable = tickOverlay(holder, resource)
                            holder.file_icon.setImageDrawable(layerDrawable)
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
                        return false
                    }
                }).into(holder.file_icon)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun tickOverlay(holder: FileManagerViewHolder, drawable: Drawable?): LayerDrawable {
        val r = holder.file_icon.resources
        val layers = arrayOfNulls<Drawable>(2)
        layers[0] = drawable
        layers[0]?.alpha = 50
        layers[1] = r.getDrawable(R.drawable.checkmark)

        return LayerDrawable(layers)
    }

    private fun setSelectedOnLoad(files: List<AboutFile>): List<AboutFile> {
        for (element in files) {
            for (selected in selectedList) {
                if (element.path == selected.path)
                    element.selected = true
            }
        }
        return files
    }

    private fun removeFromSelectedList(file: AboutFile) {
        for (position in selectedList.indices) {
            if (selectedList[position].path == file.path) {
                selectedList.removeAt(position)
                break
            }
        }
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

    fun backupSelectedList() {
        selectedListBackup = selectedList.toMutableList()
    }

    fun restoreSelectedList() {
        selectedList = selectedListBackup.toMutableList()
    }

    fun addToSelectedList(file: AboutFile) {
        selectedList.add(file)
    }

    fun addAllToSelectedList() {
        for (element in fileList)
            if (!selectedList.contains(element))
                selectedList.add(element)
    }

    inner class FileManagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var file_icon: ImageView = itemView.file_icon
        val file_name: TextView = itemView.file_name
        val file_info: TextView = itemView.file_info

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
        fun onItemLongClick(position: Int, view: View): Boolean
    }
}