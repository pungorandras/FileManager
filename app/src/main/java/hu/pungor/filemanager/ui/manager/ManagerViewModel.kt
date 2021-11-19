package hu.pungor.filemanager.ui.manager

import co.zsmb.rainbowcake.base.RainbowCakeViewModel
import javax.inject.Inject

class ManagerViewModel @Inject constructor(
    private val managerPresenter: ManagerPresenter
) : RainbowCakeViewModel<ManagerViewState>(Loading) {

    fun load() = execute {
        viewState = Loading
        try {
            viewState = ManagerReady(managerPresenter.getData())
        } catch (e: Exception) {
            viewState = Error
        }
    }

}
