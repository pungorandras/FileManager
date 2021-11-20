package hu.pungor.filemanager.ui.intro

import android.os.Bundle
import android.view.View
import android.widget.TextView
import co.zsmb.rainbowcake.base.RainbowCakeFragment
import co.zsmb.rainbowcake.dagger.getViewModelFromFactory
import co.zsmb.rainbowcake.extensions.exhaustive
import co.zsmb.rainbowcake.navigation.navigator
import hu.pungor.filemanager.R
import hu.pungor.filemanager.ui.manager.ManagerFragment
import kotlinx.android.synthetic.main.fragment_intro.*

class IntroFragment : RainbowCakeFragment<IntroViewState, IntroViewModel>() {

    override fun provideViewModel() = getViewModelFromFactory()
    override fun getViewResource() = R.layout.fragment_intro

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextImage.setOnClickListener {
            navigator?.replace(ManagerFragment())
        }

        // TODO set versionText with template
    }

    override fun onStart() {
        super.onStart()

        viewModel.load()
    }

    override fun render(viewState: IntroViewState) {
        when (viewState) {
            Loading -> {
                nextImage.isEnabled = false
            }
            FirstTime -> {
                nextImage.isEnabled = true
            }
            AlreadyShown -> {
                navigator?.replace(ManagerFragment())
            }
        }.exhaustive
    }

}
