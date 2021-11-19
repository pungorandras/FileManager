package hu.pungor.filemanager.ui.intro

import co.zsmb.rainbowcake.base.RainbowCakeViewModel
import javax.inject.Inject

class IntroViewModel @Inject constructor(
    private val introPresenter: IntroPresenter
) : RainbowCakeViewModel<IntroViewState>(Loading) {

    fun load() = execute {
        viewState = IntroReady(introPresenter.getData())
    }

}
