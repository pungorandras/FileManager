package hu.pungor.filemanager.ui.manager

sealed class ManagerViewState

object Loading : ManagerViewState()

object Error : ManagerViewState()

data class ManagerReady(val data: String = "") : ManagerViewState()
