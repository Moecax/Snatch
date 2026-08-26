package io.github.moecax.snatch.viewmodel

sealed interface DownloadEvent {
    data class UrlSubmitted(val url: String) : DownloadEvent
    data class VariantSelected(val id: String) : DownloadEvent
    data object DownloadClicked : DownloadEvent
    data object Retry : DownloadEvent
    data object Reset : DownloadEvent
}
