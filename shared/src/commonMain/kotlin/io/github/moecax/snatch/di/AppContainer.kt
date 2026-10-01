package io.github.moecax.snatch.di

import io.github.moecax.snatch.data.CobaltEndpoint
import io.github.moecax.snatch.data.CobaltResolver
import io.github.moecax.snatch.data.InMemorySettingsStore
import io.github.moecax.snatch.data.KtorLinkExpander
import io.github.moecax.snatch.data.KtorMediaDownloader
import io.github.moecax.snatch.domain.DownloadManager
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.domain.ResolverSettings
import io.github.moecax.snatch.domain.SettingsStore
import io.github.moecax.snatch.viewmodel.DownloadViewModel
import io.github.moecax.snatch.viewmodel.SettingsViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    fileSink: FileSink,
    settingsStore: SettingsStore = InMemorySettingsStore(),
    private val onDownloadStarted: () -> Unit = {},
) {
    private val resolverSettings = ResolverSettings(settingsStore)

    // Lazy: constructing an AppContainer must stay cheap and side-effect-free, since a fresh
    // one is built on every recomposition root recreation (e.g. Android config change) even
    // though the ViewModelStore usually discards it in favor of a retained ViewModel — an
    // eager HttpClient here would leak one real client per discarded container. One client is
    // shared by the resolver, the link expander, and the downloader rather than one each.
    private val httpClient by lazy { HttpClient() }
    private val linkExpander by lazy { KtorLinkExpander(httpClient) }
    private val resolverRegistry by lazy {
        val cobalt = CobaltResolver(httpClient) {
            resolverSettings.baseUrl?.let { CobaltEndpoint(it, resolverSettings.apiKey) }
        }
        ResolverRegistry(listOf(cobalt), linkExpander)
    }
    val downloadManager by lazy {
        DownloadManager(
            downloader = KtorMediaDownloader(httpClient, fileSink),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            onDownloadStarted = onDownloadStarted,
        )
    }

    fun createDownloadViewModel(): DownloadViewModel = DownloadViewModel(resolverRegistry, downloadManager)

    fun createSettingsViewModel(): SettingsViewModel = SettingsViewModel(resolverSettings)
}
