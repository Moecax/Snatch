package io.github.moecax.snatch.domain.model

data class MediaVariant(
    val id: String,
    val label: String,
    val url: String,
    val mediaType: MediaType,
    val approxSizeBytes: Long?,
    val container: String?,
    val hasAudio: Boolean = true,
)
