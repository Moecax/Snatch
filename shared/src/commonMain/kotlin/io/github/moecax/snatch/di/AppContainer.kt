package io.github.moecax.snatch.di

import io.github.moecax.snatch.data.CobaltResolver
import io.github.moecax.snatch.data.KtorLinkExpander
import io.github.moecax.snatch.data.KtorMediaDownloader
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.viewmodel.DownloadViewModel
import io.ktor.client.HttpClient

class AppContainer(
    fileSink: FileSink,
    // Point this at a self-hosted Cobalt instance (https://github.com/imputnet/cobalt) or a
    // public one from https://instances.cobalt.best that allows unauthenticated/CORS use —
    // api.cobalt.tools itself requires bot-protection/an API key and isn't meant for
    // third-party callers. ".invalid" is a reserved TLD (RFC 2606) that will never resolve,
    // so the placeholder fails loudly instead of silently hitting someone else's server —
    // override it locally to test real downloads, and don't commit a real endpoint/key here.
    private val cobaltBaseUrl: String = "https://cobalt.example.invalid",
    private val cobaltApiKey: String? = null,
) {

    // Lazy: constructing an AppContainer must stay cheap and side-effect-free, since a fresh
    // one is built on every recomposition root recreation (e.g. Android config change) even
    // though the ViewModelStore usually discards it in favor of a retained ViewModel — an
    // eager HttpClient here would leak one real client per discarded container. One client is
    // shared by the resolver, the link expander, and the downloader rather than one each.
    private val httpClient by lazy { HttpClient() }
    private val linkExpander by lazy { KtorLinkExpander(httpClient) }
    private val resolverRegistry by lazy {
        ResolverRegistry(listOf(CobaltResolver(httpClient, cobaltBaseUrl, cobaltApiKey)), linkExpander)
    }
    private val mediaDownloader by lazy { KtorMediaDownloader(httpClient, fileSink) }

    fun createDownloadViewModel(): DownloadViewModel = DownloadViewModel(resolverRegistry, mediaDownloader)
}
