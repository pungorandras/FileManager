package hu.pungor.filemanager.ui.Intro

import android.os.Bundle
import android.view.View
import co.zsmb.rainbowcake.base.RainbowCakeFragment
import co.zsmb.rainbowcake.dagger.getViewModelFromFactory
import hu.pungor.filemanager.R
import hu.pungor.filemanager.ui.intro.IntroViewModel
import hu.pungor.filemanager.ui.intro.IntroViewState

class IntroFragment : RainbowCakeFragment<IntroViewState, IntroViewModel>() {

    override fun provideViewModel() = getViewModelFromFactory()
    override fun getViewResource() = R.layout.fragment_intro

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO Setup views
    }

    override fun onStart() {
        super.onStart()

        viewModel.load()
    }

    override fun render(viewState: IntroViewState) {
        // TODO Render state
    }

}
