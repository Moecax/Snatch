package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.model.MediaType
import kotlinx.browser.document
import kotlinx.coroutines.flow.Flow
import kotlin.js.JsAny
import kotlin.js.toJsArray
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

class WebFileSink : FileSink {

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun write(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String {
        // Blob concatenates its parts itself, so each chunk is handed over as it arrives rather than
        // first being copied into one combined ByteArray — that copy doubled peak memory.
        val parts = mutableListOf<JsAny?>()
        bytes.collect { chunk -> parts += chunk.toInt8Array() }
        val blob = Blob(parts.toJsArray(), BlobPropertyBag(type = mimeType))
        val objectUrl = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = objectUrl
        anchor.download = fileName
        document.body?.appendChild(anchor)
        anchor.click()
        anchor.remove()
        URL.revokeObjectURL(objectUrl)
        return fileName
    }
}
