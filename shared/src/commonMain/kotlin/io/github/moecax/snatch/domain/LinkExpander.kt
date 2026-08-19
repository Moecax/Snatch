package io.github.moecax.snatch.domain

interface LinkExpander {
    suspend fun expand(url: String): String
}
