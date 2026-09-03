package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.MediaResolver
import io.github.moecax.snatch.domain.MediaResolverContractTest
import io.github.moecax.snatch.domain.model.SocialPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel

class CobaltResolverContractTest : MediaResolverContractTest() {
    private val mockEngine = MockEngine {
        respond(
            content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/video.mp4","filename":"video.mp4"}"""),
            status = HttpStatusCode.OK,
        )
    }

    override val resolver: MediaResolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test")
    override val supportedPlatform = SocialPlatform.YOUTUBE
    override val supportedUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    override val unsupportedPlatform = SocialPlatform.UNKNOWN
    override val unsupportedUrl = "https://example.com/some/page"
}
