package io.github.moecax.snatch.ui

import io.github.moecax.snatch.domain.model.AppError

fun AppError.toDisplayMessage(): String = when (this) {
    is AppError.UnsupportedPlatform -> "This platform isn't supported yet."
    AppError.InvalidUrl -> "That doesn't look like a valid link."
    AppError.NoNetwork -> "The connection dropped. Check your internet and try again."
    AppError.ServerUnreachable -> "Couldn't reach your Cobalt server. Check your connection and the server address in Settings."
    AppError.ResolverAuthRequired -> "This Cobalt server doesn't accept apps without an API key. Add a key or use another server in Settings."
    AppError.NotACobaltServer -> "That server address isn't a Cobalt API. Check it in Settings."
    is AppError.ResolutionFailed -> "Couldn't resolve this link."
    AppError.MediaUnavailable -> "This media is unavailable."
    AppError.ResolverNotConfigured -> "No Cobalt server set. Add one in Snatch's Settings."
    AppError.RateLimited -> "Too many requests — try again shortly."
    is AppError.StorageError -> "Couldn't save the file."
    is AppError.Unknown -> "Something went wrong."
}
