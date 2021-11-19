package hu.pungor.filemanager.di

import androidx.lifecycle.ViewModel
import co.zsmb.rainbowcake.dagger.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import hu.pungor.filemanager.ui.intro.IntroViewModel
import hu.pungor.filemanager.ui.manager.ManagerViewModel

@Suppress("unused")
@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(IntroViewModel::class)
    abstract fun bindIntroViewModel(introViewModel: IntroViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ManagerViewModel::class)
    abstract fun bindManagerViewModel(managerViewModel: ManagerViewModel): ViewModel

}
