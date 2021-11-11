package hu.pungor.filemanager.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import hu.pungor.filemanager.FileManagerActivity
import hu.pungor.filemanager.R
import kotlinx.android.synthetic.main.activity_intro_screen.*

class IntroScreenActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_screen)

        findViewById<TextView>(R.id.version).text = getString(R.string.version) +
                applicationContext.packageManager.getPackageInfo(
                    applicationContext.packageName,
                    0
                ).versionName

        next.setOnClickListener {
            startActivity(Intent(this, FileManagerActivity::class.java))
        }
    }
}