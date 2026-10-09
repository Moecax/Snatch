package io.github.moecax.snatch.domain.model

sealed interface AppError {
    val retryable: Boolean

    data class UnsupportedPlatform(val platform: SocialPlatform) : AppError {
        override val retryable: Boolean = false
    }

    data object InvalidUrl : AppError {
        override val retryable: Boolean = false
    }

    data object NoNetwork : AppError {
        override val retryable: Boolean = true
    }

    /**
     * The resolver's server couldn't be reached at all. Kept apart from [NoNetwork] because the
     * client can't tell "offline" from "wrong server address" — both surface as the same
     * connection failure — so the user needs to be pointed at Settings too.
     */
    data object ServerUnreachable : AppError {
        override val retryable: Boolean = true
    }

    data class ResolutionFailed(val reason: String?) : AppError {
        override val retryable: Boolean = true
    }

    data object MediaUnavailable : AppError {
        override val retryable: Boolean = false
    }

    /** No resolver endpoint has been set in Settings yet. */
    data object ResolverNotConfigured : AppError {
        override val retryable: Boolean = false
    }

    data object RateLimited : AppError {
        override val retryable: Boolean = true
    }

    data class StorageError(val reason: String?) : AppError {
        override val retryable: Boolean = false
    }

    data class Unknown(val throwable: Throwable?) : AppError {
        override val retryable: Boolean = false
    }
}
