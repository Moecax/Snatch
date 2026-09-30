package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.AppErrorException
import io.github.moecax.snatch.domain.MediaResolver
import io.github.moecax.snatch.domain.PlatformDetector
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.domain.model.SocialPlatform
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val cobaltJson = Json { ignoreUnknownKeys = true }

/**
 * Resolves media via a Cobalt-style third-party API (https://github.com/imputnet/cobalt).
 * [baseUrl] must point at a self-hosted instance or a public instance from
 * https://instances.cobalt.best that allows unauthenticated/CORS use — api.cobalt.tools
 * itself requires bot-protection and isn't meant for third-party callers (see its docs/api.md).
 */
class CobaltResolver(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String? = null,
) : MediaResolver {

    override fun supports(platform: SocialPlatform, url: String): Boolean = platform != SocialPlatform.UNKNOWN

    override suspend fun resolve(url: String): Result<ResolvedMedia> {
        val platform = PlatformDetector.detect(url)
        if (platform == SocialPlatform.UNKNOWN) {
            return Result.failure(AppErrorException(AppError.UnsupportedPlatform(platform)))
        }
        return try {
            val response = httpClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json")
                apiKey?.let { header(HttpHeaders.Authorization, "Api-Key $it") }
                setBody(cobaltJson.encodeToString(CobaltRequestDto.serializer(), CobaltRequestDto(url = url)))
            }
            val bodyText = response.bodyAsText()
            val dto = try {
                cobaltJson.decodeFromString(CobaltResponseDto.serializer(), bodyText)
            } catch (e: SerializationException) {
                return Result.failure(AppErrorException(mapHttpStatus(response.status)))
            }
            toResult(url, platform, dto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AppErrorException(AppError.NoNetwork))
        }
    }
}

private fun toResult(url: String, platform: SocialPlatform, dto: CobaltResponseDto): Result<ResolvedMedia> = when (dto.status) {
    "tunnel", "redirect" -> {
        val mediaUrl = dto.url
        if (mediaUrl == null) {
            Result.failure(AppErrorException(AppError.ResolutionFailed("Resolver returned no media URL")))
        } else {
            val extension = extensionOf(dto.filename)
            Result.success(
                ResolvedMedia(
                    sourceUrl = url,
                    platform = platform,
                    title = dto.filename,
                    author = null,
                    thumbnailUrl = null,
                    variants = listOf(
                        MediaVariant(
                            id = "auto",
                            label = "Best available",
                            url = mediaUrl,
                            mediaType = mediaTypeFor(extension),
                            approxSizeBytes = null,
                            container = extension,
                        ),
                    ),
                ),
            )
        }
    }
    "picker" -> {
        val items = dto.picker.orEmpty()
        if (items.isEmpty()) {
            Result.failure(AppErrorException(AppError.ResolutionFailed("Resolver returned an empty picker")))
        } else {
            Result.success(
                ResolvedMedia(
                    sourceUrl = url,
                    platform = platform,
                    title = null,
                    author = null,
                    thumbnailUrl = items.first().thumb,
                    variants = items.mapIndexed { index, item ->
                        MediaVariant(
                            id = "item-$index",
                            label = "Item ${index + 1}",
                            url = item.url,
                            mediaType = if (item.type == "video") MediaType.Video else MediaType.Image,
                            approxSizeBytes = null,
                            container = extensionOf(item.url),
                        )
                    },
                ),
            )
        }
    }
    // v1 offers muxed variants only (CLAUDE.md) — "local-processing" means the client would
    // need to merge separate audio/video streams itself, which this app deliberately doesn't
    // do yet.
    "local-processing" -> Result.failure(
        AppErrorException(AppError.ResolutionFailed("This item needs client-side processing, which Snatch doesn't support yet")),
    )
    "error" -> Result.failure(AppErrorException(mapCobaltErrorCode(dto.error?.code ?: "error.api.unknown")))
    else -> Result.failure(AppErrorException(AppError.ResolutionFailed("Unrecognized resolver response status: ${dto.status}")))
}

private fun mapHttpStatus(status: HttpStatusCode): AppError = when (status.value) {
    429 -> AppError.RateLimited
    in 500..599 -> AppError.ResolutionFailed("Resolver instance returned ${status.value}")
    else -> AppError.NoNetwork
}

// Cobalt doesn't publish an exhaustive error-code enum (see this plan's Global Constraints) —
// this buckets by substring against the "error.api.<category>.<detail>" codes observed in its
// source/issues. Adjust the buckets below if a real instance turns up a code that lands wrong.
private fun mapCobaltErrorCode(code: String): AppError = when {
    "rate" in code -> AppError.RateLimited
    "invalid" in code -> AppError.InvalidUrl
    "unavailable" in code || "private" in code || "geo" in code || "region" in code || "age" in code -> AppError.MediaUnavailable
    else -> AppError.ResolutionFailed(code)
}

private fun extensionOf(filename: String?): String? =
    filename?.substringAfterLast('.', missingDelimiterValue = "")?.ifBlank { null }

private fun mediaTypeFor(extension: String?): MediaType = when (extension?.lowercase()) {
    "mp4", "webm", "mkv", "mov" -> MediaType.Video
    "jpg", "jpeg", "png", "webp", "gif" -> MediaType.Image
    "mp3", "m4a", "ogg", "opus", "wav" -> MediaType.Audio
    else -> MediaType.Gallery
}
