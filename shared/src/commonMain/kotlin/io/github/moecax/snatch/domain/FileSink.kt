package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

interface FileSink {
    suspend fun write(fileName: String, mimeType: String, mediaType: MediaType, bytes: Flow<ByteArray>): String
}
