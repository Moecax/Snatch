package io.github.moecax.snatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.moecax.snatch.viewmodel.DownloadEvent

/**
 * Wraps an event handler so [DownloadEvent.DownloadClicked] first asks for the permission a
 * download needs, right when it's about to start: notifications on Android 13+, storage on
 * Android 9 and below. Denial doesn't block the event — without notifications the download runs
 * silently, and without storage it fails with a save error instead of doing nothing.
 * [onDownloadStarted] fires after the event has been forwarded.
 */
@Composable
fun rememberDownloadGate(
    onEvent: (DownloadEvent) -> Unit,
    onDownloadStarted: () -> Unit = {},
): (DownloadEvent) -> Unit {
    val context = LocalContext.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentOnDownloadStarted by rememberUpdatedState(onDownloadStarted)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentOnEvent(DownloadEvent.DownloadClicked)
        currentOnDownloadStarted()
    }

    return remember(context, launcher) {
        { event ->
            val missingPermission = downloadPermission()?.takeIf {
                event == DownloadEvent.DownloadClicked &&
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            when {
                missingPermission != null -> launcher.launch(missingPermission)
                event == DownloadEvent.DownloadClicked -> {
                    currentOnEvent(event)
                    currentOnDownloadStarted()
                }
                else -> currentOnEvent(event)
            }
        }
    }
}

private fun downloadPermission(): String? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.POST_NOTIFICATIONS
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    else -> null
}
