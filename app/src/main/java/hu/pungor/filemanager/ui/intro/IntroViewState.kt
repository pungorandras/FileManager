package hu.pungor.filemanager.ui.intro

sealed class IntroViewState

object Loading : IntroViewState()

data class IntroReady(val data: String = "") : IntroViewState()
