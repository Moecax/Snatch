package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.model.MediaType
import kotlinx.browser.document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
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
        val chunks = bytes.toList()
        val combined = ByteArray(chunks.sumOf { it.size })
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(combined, offset)
            offset += chunk.size
        }
        val parts = listOf<JsAny?>(combined.toInt8Array()).toJsArray()
        val blob = Blob(parts, BlobPropertyBag(type = mimeType))
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
