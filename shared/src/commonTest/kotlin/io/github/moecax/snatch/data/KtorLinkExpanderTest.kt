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
}
