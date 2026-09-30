package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.ResolvedMedia

class ResolverRegistry(
    private val resolvers: List<MediaResolver>,
    private val linkExpander: LinkExpander = LinkExpander { it },
) {

    suspend fun resolve(url: String): Result<ResolvedMedia> {
        // Expansion failure (dead shortener, network hiccup) isn't fatal on its own — fall
        // back to the original short URL and let the resolver's own request surface the
        // real error instead of masking it as an expansion problem.
        val expandedUrl = if (PlatformDetector.isShortLink(url)) {
            runCatching { linkExpander.expand(url) }.getOrDefault(url)
        } else {
            url
        }
        val platform = PlatformDetector.detect(expandedUrl)
        val resolver = resolvers.firstOrNull { it.supports(platform, expandedUrl) }
            ?: return Result.failure(AppErrorException(AppError.UnsupportedPlatform(platform)))
        return resolver.resolve(expandedUrl)
    }
}
