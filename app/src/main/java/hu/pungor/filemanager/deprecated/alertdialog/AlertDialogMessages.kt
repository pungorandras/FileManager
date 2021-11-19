package hu.pungor.filemanager.deprecated.alertdialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import hu.pungor.filemanager.deprecated.FileManagerActivity
import hu.pungor.filemanager.R

class AlertDialogMessages {

    fun noItemsSelected(activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.no_items_selected
        )
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text = activity.getString(
            R.string.to_select_items
        )

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok), null)
        builder.show()
    }

    @SuppressLint("InflateParams")
    fun alreadyExists(name: String, activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = name
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text = activity.getString(
            R.string.already_exist
        )

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok), null)
        builder.show()
    }

    @SuppressLint("InflateParams")
    fun nameIsNull(activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.empty_name
        )
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text = activity.getString(
            R.string.enter_name
        )

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok), null)
        builder.show()
    }

    @SuppressLint("InflateParams")
    fun copyOrMoveIntoItself(copyOrMove: String, activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.attention
        )
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)

        if (copyOrMove == "copy")
            customText.findViewById<TextView>(R.id.custom_text).text =
                activity.getString(R.string.cannot_copy_into_itself)
        else if (copyOrMove == "move")
            customText.findViewById<TextView>(R.id.custom_text).text =
                activity.getString(R.string.cannot_move_into_itself)

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok), null)
        builder.show()
    }

    @SuppressLint("InflateParams")
    fun noResults(activity: FileManagerActivity) {
        val customTitle =
            LayoutInflater.from(activity).inflate(R.layout.custom_title, null)
        customTitle.findViewById<TextView>(R.id.title_text).text = activity.getString(
            R.string.info
        )
        val customText =
            LayoutInflater.from(activity).inflate(R.layout.custom_text_alertdialog, null)
        customText.findViewById<TextView>(R.id.custom_text).text = activity.getString(
            R.string.no_results
        )

        val builder = AlertDialog.Builder(activity)
            .setCustomTitle(customTitle)
            .setView(customText)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.ok), null)
        builder.show()
    }
}