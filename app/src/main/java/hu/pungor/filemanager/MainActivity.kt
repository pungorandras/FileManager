package hu.pungor.filemanager

import android.os.Bundle
import co.zsmb.rainbowcake.navigation.SimpleNavActivity
import hu.pungor.filemanager.ui.intro.IntroFragment


class MainActivity : SimpleNavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            navigator.add(IntroFragment())
        }
    }

}
