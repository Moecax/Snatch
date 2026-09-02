package io.github.moecax.snatch.di

import io.github.moecax.snatch.data.FakeResolver
import io.github.moecax.snatch.data.KtorMediaDownloader
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.viewmodel.DownloadViewModel
import io.ktor.client.HttpClient

class AppContainer(fileSink: FileSink) {

    private val resolverRegistry = ResolverRegistry(listOf(FakeResolver()))

    // Lazy: constructing an AppContainer must stay cheap and side-effect-free, since a fresh
    // one is built on every recomposition root recreation (e.g. Android config change) even
    // though the ViewModelStore usually discards it in favor of a retained ViewModel — an
    // eager HttpClient here would leak one real client per discarded container.
    private val mediaDownloader by lazy { KtorMediaDownloader(HttpClient(), fileSink) }

    fun createDownloadViewModel(): DownloadViewModel = DownloadViewModel(resolverRegistry, mediaDownloader)
}
