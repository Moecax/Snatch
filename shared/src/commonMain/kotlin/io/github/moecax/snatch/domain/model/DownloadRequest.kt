package io.github.moecax.snatch.domain.model

data class DownloadRequest(
    val variant: MediaVariant,
    val resolved: ResolvedMedia,
    val suggestedFileName: String,
)
