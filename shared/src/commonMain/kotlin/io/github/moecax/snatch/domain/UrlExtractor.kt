package io.github.moecax.snatch.domain

object UrlExtractor {

    private val urlPattern = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    // Sentence punctuation that commonly hugs a pasted link ("see https://x.com/a/1.") but is
    // almost never part of one; ')' is only stripped when unbalanced, so wiki-style URLs survive.
    private const val TRAILING_PUNCTUATION = ".,;:!?\"'>]}"

    fun firstUrl(text: String): String? {
        val match = urlPattern.find(text) ?: return null
        var url = match.value
        while (url.isNotEmpty()) {
            val last = url.last()
            val stripParen = last == ')' && url.count { it == ')' } > url.count { it == '(' }
            if (last in TRAILING_PUNCTUATION || stripParen) url = url.dropLast(1) else break
        }
        return url.takeIf { it.substringAfter("://").isNotEmpty() }
    }
}
