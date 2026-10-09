package io.github.moecax.snatch.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.model.MediaType
import java.io.File
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(fileName, mimeType, mediaType, bytes)
        } else {
            writeToPublicDirectory(fileName, mimeType, mediaType, bytes)
        }
    }

    /**
     * Pre-Q MediaStore has no RELATIVE_PATH/IS_PENDING, so the file is written to an explicit public
     * path and then registered through the DATA column. Registering it (rather than returning a
     * file:// URI) yields a content:// URI that the "open file" notification can hand to other apps
     * without a FileUriExposedException.
     */
    @Suppress("DEPRECATION")
    private suspend fun writeToPublicDirectory(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String {
        check(context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            "Storage permission was denied."
        }
        val (collection, publicDir) = when (mediaType) {
            MediaType.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MOVIES
            MediaType.Image -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_PICTURES
            MediaType.Audio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MUSIC
            MediaType.Gallery -> MediaStore.Files.getContentUri("external") to Environment.DIRECTORY_DOWNLOADS
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(publicDir), "Snatch")
        check(dir.isDirectory || dir.mkdirs()) { "Could not create $dir" }
        // MediaStore's DATA column is unique, so an existing file can't simply be overwritten.
        val file = uniqueFile(dir, fileName)
        try {
            file.outputStream().use { out -> bytes.collect { chunk -> out.write(chunk) } }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.SIZE, file.length())
            }
            val uri = context.contentResolver.insert(collection, values)
                ?: error("Could not register ${file.name} with MediaStore")
            return uri.toString()
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val stem = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").let { if (it.isEmpty()) "" else ".$it" }
        return generateSequence(0) { it + 1 }
            .map { n -> File(dir, if (n == 0) fileName else "$stem ($n)$extension") }
            .first { !it.exists() }
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
