package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.ResolvedMedia

class ResolverRegistry(private val resolvers: List<MediaResolver>) {

    suspend fun resolve(url: String): Result<ResolvedMedia> {
        val platform = PlatformDetector.detect(url)
        val resolver = resolvers.firstOrNull { it.supports(platform, url) }
            ?: return Result.failure(AppErrorException(AppError.UnsupportedPlatform(platform)))
        return resolver.resolve(url)
    }
}
