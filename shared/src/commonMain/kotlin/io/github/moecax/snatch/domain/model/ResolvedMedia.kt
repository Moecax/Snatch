package io.github.moecax.snatch.domain.model

data class ResolvedMedia(
    val sourceUrl: String,
    val platform: SocialPlatform,
    val title: String?,
    val author: String?,
    val thumbnailUrl: String?,
    val variants: List<MediaVariant>,
)
