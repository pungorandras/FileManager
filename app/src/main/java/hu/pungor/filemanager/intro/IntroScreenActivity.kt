package hu.pungor.filemanager.intro

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import hu.pungor.filemanager.R
import kotlinx.android.synthetic.main.activity_intro_screen.*

@Suppress("DEPRECATION")
class IntroScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_screen)

        val version =
            getString(R.string.version) + packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.version).text = version

        next.setOnClickListener {
            this.finish()
        }
    }
}