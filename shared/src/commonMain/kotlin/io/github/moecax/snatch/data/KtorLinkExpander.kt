package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.LinkExpander
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet

class KtorLinkExpander(private val httpClient: HttpClient) : LinkExpander {

    // Ktor's default client follows redirects automatically (the HttpRedirect plugin is
    // installed unless explicitly disabled) — this never reads the response body, it just
    // lets the engine chase Location headers and reports back the final request URL once
    // headers for the last hop have arrived.
    override suspend fun expand(url: String): String =
        httpClient.prepareGet(url).execute { response -> response.call.request.url.toString() }
}
