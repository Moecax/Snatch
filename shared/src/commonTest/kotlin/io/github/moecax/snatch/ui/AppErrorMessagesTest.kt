package io.github.moecax.snatch.ui

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppErrorMessagesTest {

    private val allErrors = listOf(
        AppError.UnsupportedPlatform(SocialPlatform.UNKNOWN),
        AppError.InvalidUrl,
        AppError.NoNetwork,
        AppError.ResolutionFailed("boom"),
        AppError.MediaUnavailable,
        AppError.RateLimited,
        AppError.StorageError("disk full"),
        AppError.Unknown(null),
    )

    @Test
    fun everyAppErrorHasANonBlankDisplayMessage() {
        allErrors.forEach { error ->
            assertTrue(error.toDisplayMessage().isNotBlank(), "no message for $error")
        }
    }

    @Test
    fun messagesNeverLeakRawReasonsOrExceptionText() {
        val leaky = listOf(
            AppError.ResolutionFailed("HTTP 403 from cdn.example.net") to "HTTP 403",
            AppError.StorageError("ENOSPC /data/user/0/files") to "ENOSPC",
            AppError.Unknown(IllegalStateException("NullPointerException at Foo.kt:12")) to "NullPointerException",
        )
        leaky.forEach { (error, rawText) ->
            assertFalse(error.toDisplayMessage().contains(rawText), "$error leaks \"$rawText\"")
        }
    }
}
