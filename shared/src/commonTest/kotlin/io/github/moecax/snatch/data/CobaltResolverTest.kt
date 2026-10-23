package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.appErrorOrNull
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.SocialPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CobaltResolverTest {

    // MockEngine defaults to a real dispatcher (Dispatchers.IO), which races against runTest's
    // virtual clock: withTimeoutOrNull's timer lives on the test scheduler while the engine's
    // "response" lives on a real thread, so the scheduler sees nothing pending and fast-forwards
    // past the timeout before the mocked response ever arrives. Unconfined keeps the handler on
    // the calling (test) dispatcher so both share one clock.
    private fun mockEngineOf(handler: MockRequestHandler): MockEngine = MockEngine(
        MockEngineConfig().apply {
            requestHandlers.add(handler)
            dispatcher = Dispatchers.Unconfined
        },
    )

    private fun engineReturning(json: String, status: HttpStatusCode = HttpStatusCode.OK): MockEngine =
        mockEngineOf { respond(content = ByteReadChannel(json), status = status) }

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
    fun nonJsonClientErrorMeansTheAddressIsNotACobaltApi() = runTest {
        // What an instance's web frontend returns for a POST, e.g. cobalt.canine.tools.
        val resolver = CobaltResolver(
            HttpClient(engineReturning("", HttpStatusCode.MethodNotAllowed)),
            "https://cobalt.mock.test",
        )

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(AppError.NotACobaltServer, result.appErrorOrNull())
    }

    @Test
    fun authErrorCodesMapToResolverAuthRequired() = runTest {
        listOf("error.api.auth.jwt.missing", "error.api.auth.jwt.invalid", "error.api.auth.key.missing").forEach { code ->
            val json = """{"status":"error","error":{"code":"$code"}}"""
            val resolver = CobaltResolver(HttpClient(engineReturning(json, HttpStatusCode.BadRequest)), "https://cobalt.mock.test")

            val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

            assertEquals(AppError.ResolverAuthRequired, result.appErrorOrNull(), code)
        }
    }

    @Test
    fun connectionFailureMapsToServerUnreachable() = runTest {
        val resolver = CobaltResolver(HttpClient(mockEngineOf { throw RuntimeException("unknown host") }), "https://cobalt.mock.test")

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(AppError.ServerUnreachable, result.appErrorOrNull())
    }

    @Test
    fun unknownPlatformShortCircuitsWithoutANetworkCall() = runTest {
        var callCount = 0
        val mockEngine = mockEngineOf {
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
        val mockEngine = mockEngineOf { request ->
            // The resolver also fires a HEAD at the resolved media URL to size it afterwards —
            // that request must not carry the Cobalt API key, so only the POST is captured here.
            if (request.method == HttpMethod.Post) capturedAuth = request.headers["Authorization"]
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test", apiKey = "secret-key")

        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals("Api-Key secret-key", capturedAuth)
    }

    @Test
    fun sizeFetchDoesNotCarryTheCobaltApiKey() = runTest {
        var capturedHeadAuth: String? = null
        var sawHeadRequest = false
        val mockEngine = mockEngineOf { request ->
            if (request.method == HttpMethod.Head) {
                sawHeadRequest = true
                capturedHeadAuth = request.headers["Authorization"]
            }
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test", apiKey = "secret-key")

        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(true, sawHeadRequest)
        assertNull(capturedHeadAuth)
    }

    @Test
    fun missingEndpointFailsWithResolverNotConfiguredWithoutCallingTheNetwork() = runTest {
        var requests = 0
        val mockEngine = mockEngineOf {
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
        val mockEngine = mockEngineOf { request ->
            // Only track the resolve POST itself; the HEAD size-fetch always targets the media
            // CDN host, which would otherwise pollute this list.
            if (request.method == HttpMethod.Post) hosts += request.url.host
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        var baseUrl = "https://first.mock.test"
        val resolver = CobaltResolver(HttpClient(mockEngine)) { CobaltEndpoint(baseUrl, null) }

        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        baseUrl = "https://second.mock.test"
        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(listOf("first.mock.test", "second.mock.test"), hosts)
    }

    @Test
    fun preferredVideoQualityIsSentInTheRequestBody() = runTest {
        var capturedBody: String? = null
        val mockEngine = mockEngineOf { request ->
            if (request.method == HttpMethod.Post) capturedBody = request.body.toByteArray().decodeToString()
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test", videoQuality = "480")

        resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertEquals(true, capturedBody?.contains("\"videoQuality\":\"480\""))
    }

    @Test
    fun variantGetsApproxSizeFromContentLengthHeaderOnTheMediaUrl() = runTest {
        val mockEngine = mockEngineOf { request ->
            if (request.method == HttpMethod.Head) {
                respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentLength, "123456"))
            } else {
                respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
            }
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test")

        val media = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ").getOrThrow()

        assertEquals(123456L, media.variants.single().approxSizeBytes)
    }

    @Test
    fun approxSizeStaysNullWhenTheMediaHostRejectsHead() = runTest {
        val mockEngine = mockEngineOf { request ->
            if (request.method == HttpMethod.Head) {
                throw RuntimeException("405")
            }
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test")

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertNull(result.getOrThrow().variants.single().approxSizeBytes)
    }

    @Test
    fun approxSizeFetchTimesOutInsteadOfHangingTheResolve() = runTest {
        val mockEngine = mockEngineOf { request ->
            if (request.method == HttpMethod.Head) delay(Long.MAX_VALUE)
            respond(content = ByteReadChannel("""{"status":"tunnel","url":"https://cdn.example.com/a.mp4","filename":"a.mp4"}"""), status = HttpStatusCode.OK)
        }
        val resolver = CobaltResolver(HttpClient(mockEngine), "https://cobalt.mock.test")

        val result = resolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertNull(result.getOrThrow().variants.single().approxSizeBytes)
    }

    @Test
    fun testConnectionSucceedsForAPlainServerInfoResponse() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("""{"cobalt":{"version":"10.2.0"}}""")),
            "https://cobalt.mock.test",
        )

        val result = resolver.testConnection("https://cobalt.mock.test", apiKey = null)

        assertEquals("10.2.0", result.getOrThrow())
    }

    @Test
    fun testConnectionFlagsATurnstileOnlyInstanceWithoutAnApiKey() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("""{"cobalt":{"version":"10.2.0","turnstileSitekey":"0x123"}}""")),
            "https://cobalt.mock.test",
        )

        val result = resolver.testConnection("https://cobalt.mock.test", apiKey = null)

        assertEquals(AppError.ResolverAuthRequired, result.appErrorOrNull())
    }

    @Test
    fun testConnectionAcceptsATurnstileInstanceWhenAnApiKeyIsProvided() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("""{"cobalt":{"version":"10.2.0","turnstileSitekey":"0x123"}}""")),
            "https://cobalt.mock.test",
        )

        val result = resolver.testConnection("https://cobalt.mock.test", apiKey = "secret-key")

        assertEquals("10.2.0", result.getOrThrow())
    }

    @Test
    fun testConnectionFailsWhenTheAddressIsNotACobaltApi() = runTest {
        val resolver = CobaltResolver(
            HttpClient(engineReturning("""{"status":"ok"}""")),
            "https://cobalt.mock.test",
        )

        val result = resolver.testConnection("https://cobalt.mock.test", apiKey = null)

        assertEquals(AppError.NotACobaltServer, result.appErrorOrNull())
    }

    @Test
    fun testConnectionFailsWithServerUnreachableOnConnectionFailure() = runTest {
        val resolver = CobaltResolver(HttpClient(mockEngineOf { throw RuntimeException("unknown host") }), "https://cobalt.mock.test")

        val result = resolver.testConnection("https://cobalt.mock.test", apiKey = null)

        assertEquals(AppError.ServerUnreachable, result.appErrorOrNull())
    }
}
