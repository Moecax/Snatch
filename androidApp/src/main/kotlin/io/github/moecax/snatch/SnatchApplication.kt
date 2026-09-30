package io.github.moecax.snatch

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.moecax.snatch.data.AndroidFileSink
import io.github.moecax.snatch.di.AppContainer

class SnatchApplication : Application() {

    // Shared by both activities and the download service so they all observe the same DownloadManager.
    val container: AppContainer by lazy {
        AppContainer(
            fileSink = AndroidFileSink(this),
            onDownloadStarted = ::startDownloadService,
        )
    }

    private fun startDownloadService() {
        try {
            ContextCompat.startForegroundService(this, Intent(this, DownloadService::class.java))
        } catch (e: RuntimeException) {
            // e.g. ForegroundServiceStartNotAllowedException when the app isn't in the foreground.
            // The download still runs in the manager's scope, just without process protection.
        }
    }
}
