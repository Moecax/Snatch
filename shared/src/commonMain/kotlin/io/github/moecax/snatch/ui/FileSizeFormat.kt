package io.github.moecax.snatch.ui

import kotlin.math.round

/** Human-readable approximate size, e.g. "4.2 MB", or null when [bytes] is unknown. */
fun formatApproxSize(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    val rounded = round(value * 10) / 10.0
    val formatted = if (unitIndex == 0) rounded.toInt().toString() else rounded.toString()
    return "$formatted ${units[unitIndex]}"
}
