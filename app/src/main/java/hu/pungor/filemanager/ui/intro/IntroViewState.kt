package hu.pungor.filemanager.ui.intro

sealed class IntroViewState

object Loading : IntroViewState()

object FirstTime : IntroViewState()

object AlreadyShown : IntroViewState()

