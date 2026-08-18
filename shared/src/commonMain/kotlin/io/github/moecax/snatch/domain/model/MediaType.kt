package io.github.moecax.snatch.domain.model

sealed interface MediaType {
    data object Image : MediaType
    data object Video : MediaType
    data object Audio : MediaType
    data object Gallery : MediaType
}
