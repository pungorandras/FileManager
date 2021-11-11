package hu.pungor.filemanager.intro

import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import kotlinx.android.synthetic.main.activity_filemanager.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.backgrounds.RectanglePromptBackground
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal

class TapTargetPromptInstructions {
    var index = 0

    fun showTutorial(activity: FileManagerActivity) {
        val buttons = listOf(
            listOf(
                R.id.switch_source,
                activity.getString(R.string.switch_source),
                activity.getString(R.string.switch_source_msg)
            ),
            listOf(
                R.id.create_textfile,
                activity.getString(R.string.create_new_textfile),
                activity.getString(R.string.create_new_txt_msg)
            ),
            listOf(
                R.id.create_folder,
                activity.getString(R.string.create_new_folder),
                activity.getString(R.string.create_new_folder_msg)
            ),
            listOf(
                R.id.select_all,
                activity.getString(R.string.select_all_items),
                activity.getString(R.string.select_all_items_msg)
            ),
            listOf(
                R.id.delete_selected,
                activity.getString(R.string.delete_selected_items),
                activity.getString(R.string.delete_selected_items_msg)
            ),
            listOf(
                R.id.copy_selected,
                activity.getString(R.string.copy_selected_items),
                activity.getString(R.string.copy_selected_items_msg)
            ),
            listOf(
                R.id.move_selected,
                activity.getString(R.string.move_selected_items),
                activity.getString(R.string.move_selected_items_msg)
            ),
            listOf(
                R.id.search,
                activity.getString(R.string.search_recursively),
                activity.getString(R.string.search_recursively_msg)
            )
        )

        MaterialTapTargetPrompt.Builder(activity).apply {
            try {
                setTarget(buttons[index][0] as Int)
                primaryText = buttons[index][1] as String
                secondaryText = buttons[index][2] as String
                backgroundColour = activity.resources.getColor(R.color.colorPrimary)
                captureTouchEventOnFocal = true
                if (buttons[index][0] == R.id.switch_source) {
                    promptBackground = RectanglePromptBackground()
                    promptFocal = RectanglePromptFocal()
                }
                setPromptStateChangeListener { prompt, state ->
                    if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                        index++
                        showTutorial(activity)
                    }
                }
            } catch (e: Exception) {
                return@apply
            }
        }.show()
    }
}