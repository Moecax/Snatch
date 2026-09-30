package io.github.moecax.snatch

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.moecax.snatch.domain.DownloadManager
import io.github.moecax.snatch.domain.DownloadProgress
import io.github.moecax.snatch.domain.TrackedDownload
import io.github.moecax.snatch.ui.toDisplayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while downloads run and renders their progress. The downloads themselves
 * run in [DownloadManager]'s scope; this service only observes them, so it can stop and restart
 * freely between downloads.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private var lastStartId = 0

    private val manager: DownloadManager
        get() = (application as SnatchApplication).container.downloadManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    // The type constant is inlined and ignored below API 29, where foreground service types don't exist.
    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Must be called promptly after startForegroundService() on every start, even if we're
        // about to stop because there's nothing active.
        ServiceCompat.startForeground(
            this,
            FOREGROUND_ID,
            foregroundNotification(manager.downloads.value.values.filter { it.isActive }),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (observer == null) {
            observer = scope.launch {
                // Delay in the collector body throttles notification updates (Android rate-limits
                // them) while still always seeing the latest state afterwards.
                manager.downloads.collect { downloads ->
                    render(downloads.values)
                    delay(UPDATE_INTERVAL_MS)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(downloads: Collection<TrackedDownload>) {
        downloads.filterNot { it.isActive }.forEach { finished ->
            notifyResult(finished)
            manager.acknowledge(finished.id)
        }
        val active = downloads.filter { it.isActive }
        if (active.isEmpty()) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(lastStartId)
        } else {
            notify(FOREGROUND_ID, foregroundNotification(active))
        }
    }

    private fun foregroundNotification(active: List<TrackedDownload>): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
        val single = active.singleOrNull()
        if (single == null) {
            builder.setContentTitle(if (active.isEmpty()) "Starting download…" else "Downloading ${active.size} files")
                .setProgress(0, 0, true)
        } else {
            builder.setContentTitle(single.request.resolved.title ?: single.request.suggestedFileName)
                .setContentText(single.request.variant.label)
            val progress = single.progress as? DownloadProgress.InProgress
            val total = progress?.total
            if (progress != null && total != null && total > 0) {
                builder.setProgress(PROGRESS_MAX, (progress.bytes * PROGRESS_MAX / total).toInt(), false)
            } else {
                builder.setProgress(0, 0, true)
            }
        }
        return builder.build()
    }

    private fun notifyResult(download: TrackedDownload) {
        val title = download.request.resolved.title ?: download.request.suggestedFileName
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setAutoCancel(true)
        when (val progress = download.progress) {
            is DownloadProgress.Done -> builder
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentText("Download complete")
                .setContentIntent(openFileIntent(download.id, progress.fileUri))
            is DownloadProgress.Error -> builder
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentText(progress.cause.toDisplayMessage())
                .setContentIntent(openAppIntent())
            is DownloadProgress.InProgress -> return
        }
        notify(RESULT_ID_BASE + download.id.toInt(), builder.build())
    }

    private fun notify(id: Int, notification: Notification) {
        // Denied POST_NOTIFICATIONS just means no visible notification; the download is unaffected.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    private fun openFileIntent(downloadId: Long, fileUri: String): PendingIntent {
        val uri = Uri.parse(fileUri)
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, contentResolver.getType(uri))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this,
            downloadId.toInt(),
            view,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private val TrackedDownload.isActive: Boolean
        get() = progress is DownloadProgress.InProgress

    private companion object {
        const val CHANNEL_ID = "downloads"
        const val FOREGROUND_ID = 1
        const val RESULT_ID_BASE = 1000
        const val PROGRESS_MAX = 1000
        const val UPDATE_INTERVAL_MS = 500L
    }
}
