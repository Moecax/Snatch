package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DesktopFileSink : FileSink {

    override suspend fun write(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String = withContext(Dispatchers.IO) {
        val downloadsDir = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
        val file = File(downloadsDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bytes.collect { chunk -> out.write(chunk) }
            }
        } catch (e: Exception) {
            // Don't leave a truncated file behind for a failed/cancelled download.
            file.delete()
            throw e
        }
        file.absolutePath
    }
}
