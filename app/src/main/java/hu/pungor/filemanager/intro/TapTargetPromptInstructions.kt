package hu.pungor.filemanager.intro

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal


var index = 0

fun FileManagerActivity.showTutorial() {
    val buttons = listOf(
        listOf(
            R.id.switch_source,
            getString(R.string.switch_source),
            getString(R.string.switch_source_msg)
        ),
        listOf(
            R.id.create_textfile,
            getString(R.string.create_new_textfile),
            getString(R.string.create_new_txt_msg)
        ),
        listOf(
            R.id.create_folder,
            getString(R.string.create_new_folder),
            getString(R.string.create_new_folder_msg)
        ),
        listOf(
            R.id.select_all,
            getString(R.string.select_all_items),
            getString(R.string.select_all_items_msg)
        ),
        listOf(
            R.id.delete_selected,
            getString(R.string.delete_selected_items),
            getString(R.string.delete_selected_items_msg)
        ),
        listOf(
            R.id.copy_selected,
            getString(R.string.copy_selected_items),
            getString(R.string.copy_selected_items_msg)
        ),
        listOf(
            R.id.move_selected,
            getString(R.string.move_selected_items),
            getString(R.string.move_selected_items_msg)
        ),
        listOf(
            R.id.search,
            getString(R.string.search_recursively),
            getString(R.string.search_recursively_msg)
        )
    )

    MaterialTapTargetPrompt.Builder(this).apply {
        try {
            setTarget(buttons[index][0] as Int)
            primaryText = buttons[index][1] as String
            secondaryText = buttons[index][2] as String
            backgroundColour = resources.getColor(R.color.colorPrimary)
            captureTouchEventOnFocal = true
            if (buttons[index][0] == R.id.switch_source)
                promptFocal = RectanglePromptFocal()
            setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                    index++
                    showTutorial()
                }
            }
        } catch (e: Exception) {
            return@apply
        }
    }.show()
}

fun FileManagerActivity.loadIntroScreen() {
    val prefs = getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)
    val firstStart = prefs.getBoolean("firstStart", true)

    if (firstStart)
        startActivity(Intent(this, IntroScreenActivity::class.java))

    val editor = prefs.edit()
    editor.putBoolean("firstStart", false)
    editor.apply()
}

fun FileManagerActivity.loadTutorial() {
    val prefs = getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)
    val firstStart = prefs.getBoolean("tutorial", true)

    if (firstStart)
        showTutorial()

    val editor = prefs.edit()
    editor.putBoolean("tutorial", false)
    editor.apply()
}
