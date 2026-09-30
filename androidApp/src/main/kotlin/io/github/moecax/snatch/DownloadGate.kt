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
 * Wraps an event handler so [DownloadEvent.DownloadClicked] first asks for the notification
 * permission (Android 13+) right when a download is about to start. Denial doesn't block the
 * download — it just runs without a visible notification. [onDownloadStarted] fires after the
 * event has been forwarded.
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
            val needsPermission = event == DownloadEvent.DownloadClicked &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            when {
                needsPermission -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                event == DownloadEvent.DownloadClicked -> {
                    currentOnEvent(event)
                    currentOnDownloadStarted()
                }
                else -> currentOnEvent(event)
            }
        }
    }
}
