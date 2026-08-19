package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolverRegistryTest {

    private class StubResolver(
        private val platform: SocialPlatform,
        private val result: Result<ResolvedMedia>,
    ) : MediaResolver {
        override fun supports(platform: SocialPlatform, url: String) = platform == this.platform
        override suspend fun resolve(url: String): Result<ResolvedMedia> = result
    }

    private val sampleMedia = ResolvedMedia(
        sourceUrl = "https://www.youtube.com/watch?v=abc",
        platform = SocialPlatform.YOUTUBE,
        title = "Sample",
        author = "Someone",
        thumbnailUrl = null,
        variants = listOf(
            MediaVariant(
                id = "1080p",
                label = "1080p",
                url = "https://cdn.example.com/video.mp4",
                mediaType = MediaType.Video,
                approxSizeBytes = null,
                container = "mp4",
            ),
        ),
    )

    @Test
    fun dispatchesToMatchingResolver() = runTest {
        val registry = ResolverRegistry(listOf(StubResolver(SocialPlatform.YOUTUBE, Result.success(sampleMedia))))

        val result = registry.resolve("https://www.youtube.com/watch?v=abc")

        assertEquals(sampleMedia, result.getOrNull())
    }

    @Test
    fun returnsUnsupportedPlatformWhenNoResolverMatches() = runTest {
        val registry = ResolverRegistry(listOf(StubResolver(SocialPlatform.TIKTOK, Result.success(sampleMedia))))

        val result = registry.resolve("https://www.youtube.com/watch?v=abc")

        assertEquals(AppError.UnsupportedPlatform(SocialPlatform.YOUTUBE), result.appErrorOrNull())
    }
}
