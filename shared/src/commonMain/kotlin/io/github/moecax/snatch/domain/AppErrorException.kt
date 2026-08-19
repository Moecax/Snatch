package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError

class AppErrorException(val appError: AppError) : Exception()

fun Result<*>.appErrorOrNull(): AppError? =
    (exceptionOrNull() as? AppErrorException)?.appError
