package io.github.moecax.snatch.ui

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlin.test.Test
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
}
