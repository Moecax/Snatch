package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.appErrorOrNull
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.SocialPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CobaltResolverTest {

    private fun engineReturning(json: String, status: HttpStatusCode = HttpStatusCode.OK): MockEngine =
        MockEngine { respond(content = ByteReadChannel(json), status = status) }

    @Test
    fun tunnelResponseProducesASingleVideoVariant() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}""")),
            "https://cobalt.mock.test",
        )

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        val media = result.getOrThrow()
        assertEquals(1, media.variants.size)
        val variant = media.variants.single()
        assertEquals("https://cdn.example.com/a.mp4", variant.url)
        assertEquals(MediaType.Video, variant.mediaType)
        assertEquals("mp4", variant.container)
    }

    @Test
    fun redirectResponseIsTreatedTheSameAsTunnel() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("""{"status":"redirect","url":"https://cdn.example.com/a.jpg","filename":"a.jpg"}""")),
            "https://cobalt.mock.test",
        )

        val result = resolver.resolve("https://www.instagram.com/p/Cabcdefghij/")

        val variant = result.getOrThrow().variants.single()
        assertEquals(MediaType.Image, variant.mediaType)
    }

    @Test
    fun pickerResponseProducesOneVariantPerItem() = runTest {
        val json = """
            {"status":"picker","picker":[
                {"type":"photo","url":"https://cdn.example.com/1.jpg","thumb":"https://cdn.example.com/1t.jpg"},
                {"type":"video","url":"https://cdn.example.com/2.mp4"}
            ]}
        """.trimIndent()
        val resolver = CobaltResolver(HttpClient(engineReturning(json)), "https://cobalt.mock.test")

        val media = resolver.resolve("https://twitter.com/someuser/status/123").getOrThrow()

        assertEquals(2, media.variants.size)
        assertEquals(MediaType.Image, media.variants[0].mediaType)
        assertEquals(MediaType.Video, media.variants[1].mediaType)
        assertEquals("https://cdn.example.com/1t.jpg", media.thumbnailUrl)
    }

    @Test
    fun localProcessingResponseFailsWithResolutionFailed() = runTest {
        val json = """{"status":"local-processing","type":"merge","service":"youtube","tunnel":["https://cdn.example.com/v.mp4"],"output":{"type":"video/mp4","filename":"a.mp4"},"audio":{"copy":true,"format":"mp3","bitrate":"128"}}"""
        val resolver = CobaltResolver(HttpClient(engineReturning(json)), "https://cobalt.mock.test")

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertIs<AppError.ResolutionFailed>(result.appErrorOrNull())
    }

    @Test
    fun rateLimitedErrorCodeMapsToRateLimited() = runTest {
        val json = """{"status":"error","error":{"code":"error.api.rate_exceeded"}}"""
        val resolver = CobaltResolver(HttpClient(engineReturning(json)), "https://cobalt.mock.test")

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(AppError.RateLimited, result.appErrorOrNull())
    }

    @Test
    fun unavailableContentErrorCodeMapsToMediaUnavailable() = runTest {
        val json = """{"status":"error","error":{"code":"error.api.content.video.unavailable"}}"""
        val resolver = CobaltResolver(HttpClient(engineReturning(json)), "https://cobalt.mock.test")

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(AppError.MediaUnavailable, result.appErrorOrNull())
    }

    @Test
    fun malformedJsonBodyFallsBackToHttpStatusMapping() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("<html>Bad Gateway</html>", HttpStatusCode.BadGateway)),
            "https://cobalt.mock.test",
        )

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertIs<AppError.ResolutionFailed>(result.appErrorOrNull())
    }

    @Test
    fun unknownPlatformShortCircuitsWithoutANetworkCall() = runTest {
        var callCount = 0
        val mockEngine = MockEngine {
            callCount++
            respond(content = ByteReadChannel("""{"status":"error","error":{"code":"x"}}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test")

        val result = resolver.resolve("https://example.com/some/page")

        assertEquals(AppError.UnsupportedPlatform(SocialPlatform.UNKNOWN), result.appErrorOrNull())
        assertEquals(0, callCount)
    }

    @Test
    fun apiKeyIsSentAsAuthorizationHeaderWhenProvided() = runTest {
        var capturedAuth: String? = null
        val mockEngine = MockEngine { request ->
            capturedAuth = request.headers["Authorization"]
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test", apiKey = "secret-key")

        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals("Api-Key secret-key", capturedAuth)
    }

    @Test
    fun missingEndpointFailsWithResolverNotConfiguredWithoutCallingTheNetwork() = runTest {
        var requests = 0
        val mockEngine = MockEngine {
            requests++
            respond(content = ByteReadChannel("{}"), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine)) { null }

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(AppError.ResolverNotConfigured, result.appErrorOrNull())
        assertEquals(0, requests)
    }

    @Test
    fun endpointIsReadOnEveryResolve() = runTest {
        val hosts = mutableListOf<String>()
        val mockEngine = MockEngine { request ->
            hosts += request.url.host
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        var baseUrl = "https://first.mock.test"
        val resolver = CobaltResolver(HttpClient(mockEngine)) { CobaltEndpoint(baseUrl, null) }

        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        baseUrl = "https://second.mock.test"
        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(listOf("first.mock.test", "second.mock.test"), hosts)
    }
}
