package hu.pungor.filemanager.ui.intro

import co.zsmb.rainbowcake.withIOContext
import javax.inject.Inject

class IntroPresenter @Inject constructor() {

    suspend fun getData(): String = withIOContext {
        ""
    }

}
