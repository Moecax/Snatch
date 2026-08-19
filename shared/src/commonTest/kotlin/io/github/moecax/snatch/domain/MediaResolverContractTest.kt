package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class MediaResolverContractTest {

    abstract val resolver: MediaResolver
    abstract val supportedPlatform: SocialPlatform
    abstract val supportedUrl: String
    abstract val unsupportedPlatform: SocialPlatform
    abstract val unsupportedUrl: String

    @Test
    fun supportsIsTrueForItsSupportedPlatform() {
        assertTrue(resolver.supports(supportedPlatform, supportedUrl))
    }

    @Test
    fun supportsIsFalseForAnUnsupportedPlatform() {
        assertFalse(resolver.supports(unsupportedPlatform, unsupportedUrl))
    }

    @Test
    fun resolveSucceedsForASupportedUrl() = runTest {
        val result = resolver.resolve(supportedUrl)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().variants.isNotEmpty())
    }

    @Test
    fun resolveFailsWithUnsupportedPlatformForAnUnsupportedUrl() = runTest {
        val result = resolver.resolve(unsupportedUrl)

        assertEquals(AppError.UnsupportedPlatform(unsupportedPlatform), result.appErrorOrNull())
    }
}
