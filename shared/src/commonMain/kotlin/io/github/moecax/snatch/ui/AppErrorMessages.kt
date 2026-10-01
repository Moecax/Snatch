package io.github.moecax.snatch.ui

import io.github.moecax.snatch.domain.model.AppError

fun AppError.toDisplayMessage(): String = when (this) {
    is AppError.UnsupportedPlatform -> "This platform isn't supported yet."
    AppError.InvalidUrl -> "That doesn't look like a valid link."
    AppError.NoNetwork -> "No internet connection."
    is AppError.ResolutionFailed -> "Couldn't resolve this link."
    AppError.MediaUnavailable -> "This media is unavailable."
    AppError.ResolverNotConfigured -> "No Cobalt server set. Add one in Snatch's Settings."
    AppError.RateLimited -> "Too many requests — try again shortly."
    is AppError.StorageError -> "Couldn't save the file."
    is AppError.Unknown -> "Something went wrong."
}
