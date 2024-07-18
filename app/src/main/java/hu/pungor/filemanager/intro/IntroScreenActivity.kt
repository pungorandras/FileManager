package hu.pungor.filemanager.intro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hu.pungor.filemanager.R
import hu.pungor.filemanager.databinding.ActivityIntroScreenBinding

class IntroScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val version =
            getString(R.string.version) + packageManager.getPackageInfo(packageName, 0).versionName
        binding.version.text = version

        binding.next.setOnClickListener {
            finish()
        }
    }
}