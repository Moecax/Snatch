package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.AppErrorException
import io.github.moecax.snatch.domain.MediaResolver
import io.github.moecax.snatch.domain.PlatformDetector
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.domain.model.SocialPlatform

class FakeResolver : MediaResolver {

    override fun supports(platform: SocialPlatform, url: String): Boolean =
        platform == SocialPlatform.YOUTUBE || platform == SocialPlatform.INSTAGRAM

    override suspend fun resolve(url: String): Result<ResolvedMedia> {
        return when (val platform = PlatformDetector.detect(url)) {
            SocialPlatform.YOUTUBE -> Result.success(sampleVideo(url))
            SocialPlatform.INSTAGRAM -> Result.success(sampleImage(url))
            else -> Result.failure(AppErrorException(AppError.UnsupportedPlatform(platform)))
        }
    }

    private fun sampleVideo(url: String) = ResolvedMedia(
        sourceUrl = url,
        platform = SocialPlatform.YOUTUBE,
        title = "Sample Video",
        author = "Sample Creator",
        thumbnailUrl = "https://picsum.photos/seed/snatch-video/400/225",
        variants = listOf(
            MediaVariant(
                id = "1080p",
                label = "1080p",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                mediaType = MediaType.Video,
                approxSizeBytes = 158_000_000,
                container = "mp4",
            ),
            MediaVariant(
                id = "720p",
                label = "720p",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                mediaType = MediaType.Video,
                approxSizeBytes = 72_000_000,
                container = "mp4",
            ),
            MediaVariant(
                id = "480p",
                label = "480p",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                mediaType = MediaType.Video,
                approxSizeBytes = 35_000_000,
                container = "mp4",
            ),
        ),
    )

    private fun sampleImage(url: String) = ResolvedMedia(
        sourceUrl = url,
        platform = SocialPlatform.INSTAGRAM,
        title = null,
        author = "Sample Creator",
        thumbnailUrl = "https://picsum.photos/seed/snatch-image/400/400",
        variants = listOf(
            MediaVariant(
                id = "original",
                label = "Original",
                url = "https://picsum.photos/seed/snatch-image/1080/1080",
                mediaType = MediaType.Image,
                approxSizeBytes = 2_400_000,
                container = "jpg",
            ),
        ),
    )
}
