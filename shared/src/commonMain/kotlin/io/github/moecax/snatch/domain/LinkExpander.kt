package io.github.moecax.snatch.domain

fun interface LinkExpander {
    suspend fun expand(url: String): String
}
