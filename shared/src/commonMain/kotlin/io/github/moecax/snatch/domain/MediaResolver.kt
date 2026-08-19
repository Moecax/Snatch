package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.domain.model.SocialPlatform

interface MediaResolver {
    fun supports(platform: SocialPlatform, url: String): Boolean
    suspend fun resolve(url: String): Result<ResolvedMedia>
}
