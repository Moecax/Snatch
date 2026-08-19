package io.github.moecax.snatch.di

import io.github.moecax.snatch.data.FakeResolver
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.viewmodel.DownloadViewModel

class AppContainer {

    private val resolverRegistry = ResolverRegistry(listOf(FakeResolver()))

    fun createDownloadViewModel(): DownloadViewModel = DownloadViewModel(resolverRegistry)
}
