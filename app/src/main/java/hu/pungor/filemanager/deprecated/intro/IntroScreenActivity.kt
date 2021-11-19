package hu.pungor.filemanager.deprecated.intro

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import hu.pungor.filemanager.R
import kotlinx.android.synthetic.main.activity_intro_screen.*

class IntroScreenActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_screen)

        findViewById<TextView>(R.id.version).text = getString(R.string.version) +
                this.packageManager.getPackageInfo(
                    this.packageName, 0
                ).versionName

        next.setOnClickListener {
            this.finish()
        }
    }
}