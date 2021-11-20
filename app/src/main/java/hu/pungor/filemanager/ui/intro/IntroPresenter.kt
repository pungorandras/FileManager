package hu.pungor.filemanager.ui.intro

import co.zsmb.rainbowcake.withIOContext
import hu.pungor.filemanager.domain.SharedPreferencesInteractor
import javax.inject.Inject

class IntroPresenter @Inject constructor() {

    suspend fun isFirstTime(): Boolean = withIOContext {
        return@withIOContext SharedPreferencesInteractor.exists(SharedPreferencesInteractor.FIRST_TIME)
    }

    suspend fun setFirstTimeFalse() = withIOContext {
        SharedPreferencesInteractor.store(SharedPreferencesInteractor.FIRST_TIME, false)
    }

}
