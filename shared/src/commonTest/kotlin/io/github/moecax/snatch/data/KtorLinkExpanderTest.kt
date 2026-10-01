package io.github.moecax.snatch.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KtorLinkExpanderTest {

    @Test
    fun expandFollowsRedirectsToTheFinalUrl() = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.toString() == "https://t.co/abcXYZ123") {
                respond(
                    content = ByteReadChannel(ByteArray(0)),
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
                )
            } else {
                respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.OK)
            }
        }
        val expander = KtorLinkExpander(HttpClient(mockEngine))

        val expanded = expander.expand("https://t.co/abcXYZ123")

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", expanded)
    }

    @Test
    fun expandFollowsMultipleHops() = runTest {
        val hops = mapOf(
            "https://bit.ly/abc" to "https://t.co/def",
            "https://t.co/def" to "https://x.com/someuser/status/1",
        )
        val mockEngine = MockEngine { request ->
            val next = hops[request.url.toString()]
            if (next != null) {
                respond(
                    content = ByteReadChannel(ByteArray(0)),
                    status = HttpStatusCode.MovedPermanently,
                    headers = headersOf(HttpHeaders.Location, next),
                )
            } else {
                respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.OK)
            }
        }

        val expanded = KtorLinkExpander(HttpClient(mockEngine)).expand("https://bit.ly/abc")

        assertEquals("https://x.com/someuser/status/1", expanded)
    }

    @Test
    fun expandReturnsTheSameUrlWhenThereIsNoRedirect() = runTest {
        val mockEngine = MockEngine { respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.OK) }

        val expanded = KtorLinkExpander(HttpClient(mockEngine)).expand("https://youtu.be/dQw4w9WgXcQ")

        assertEquals("https://youtu.be/dQw4w9WgXcQ", expanded)
    }

    @Test
    fun expandPropagatesTransportFailuresSoTheRegistryCanFallBack() = runTest {
        val mockEngine = MockEngine { throw IllegalStateException("connection reset") }

        assertFailsWith<IllegalStateException> { KtorLinkExpander(HttpClient(mockEngine)).expand("https://t.co/abc") }
    }
}
