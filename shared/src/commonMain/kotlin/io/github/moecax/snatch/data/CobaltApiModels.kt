package io.github.moecax.snatch.data

import kotlinx.serialization.Serializable

@Serializable
data class CobaltRequestDto(
    val url: String,
    val videoQuality: String = "1080",
    val downloadMode: String = "auto",
    val filenameStyle: String = "basic",
)

@Serializable
data class CobaltResponseDto(
    val status: String,
    val url: String? = null,
    val filename: String? = null,
    val error: CobaltErrorDto? = null,
    val picker: List<CobaltPickerItemDto>? = null,
)

@Serializable
data class CobaltErrorDto(val code: String)

@Serializable
data class CobaltPickerItemDto(
    val type: String,
    val url: String,
    val thumb: String? = null,
)

/** Response shape of a GET to a Cobalt instance's base URL (its server-info endpoint). */
@Serializable
data class CobaltServerInfoDto(val cobalt: CobaltInfoDto? = null)

@Serializable
data class CobaltInfoDto(
    val version: String? = null,
    /** Present when the instance requires a browser-solved Turnstile challenge for API calls. */
    val turnstileSitekey: String? = null,
)
