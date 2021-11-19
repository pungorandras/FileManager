package hu.pungor.filemanager.ui.manager

import co.zsmb.rainbowcake.withIOContext
import javax.inject.Inject

class ManagerPresenter @Inject constructor() {

    suspend fun getData(): String = withIOContext {
        ""
    }

}
