package io.github.moecax.snatch.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class AndroidFileSink(private val context: Context) : FileSink {

    override suspend fun write(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String = withContext(Dispatchers.IO) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Saving downloads requires Android 10 (API 29) or higher."
        }
        // Extracted so @RequiresApi gives Lint's NewApi check something to see — the runtime
        // `check()` above is invisible to static analysis.
        writeViaMediaStore(fileName, mimeType, mediaType, bytes)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun writeViaMediaStore(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String {
        val (collection, relativeDir) = when (mediaType) {
            MediaType.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MOVIES
            MediaType.Image -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_PICTURES
            MediaType.Audio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MUSIC
            MediaType.Gallery -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_DOWNLOADS
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$relativeDir/Snatch")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: error("Could not create a destination for $fileName")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                bytes.collect { chunk -> out.write(chunk) }
            } ?: error("Could not open output stream for $fileName")
        } catch (e: Exception) {
            // Without this, a failed/cancelled download leaves a permanent IS_PENDING=1
            // MediaStore row pointing at a truncated file — invisible in any gallery app but
            // never cleaned up.
            resolver.delete(uri, null, null)
            throw e
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri.toString()
    }
}
