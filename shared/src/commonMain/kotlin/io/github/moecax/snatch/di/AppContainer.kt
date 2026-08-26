package io.github.moecax.snatch.di

import io.github.moecax.snatch.data.FakeResolver
import io.github.moecax.snatch.data.KtorMediaDownloader
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.viewmodel.DownloadViewModel
import io.ktor.client.HttpClient

class AppContainer(fileSink: FileSink) {

    private val resolverRegistry = ResolverRegistry(listOf(FakeResolver()))
    private val mediaDownloader = KtorMediaDownloader(HttpClient(), fileSink)

    fun createDownloadViewModel(): DownloadViewModel = DownloadViewModel(resolverRegistry, mediaDownloader)
}
