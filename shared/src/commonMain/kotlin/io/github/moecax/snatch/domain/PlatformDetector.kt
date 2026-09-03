package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.SocialPlatform

object PlatformDetector {

    private val shortenerHosts = setOf(
        "t.co", "bit.ly", "tinyurl.com",
        "vm.tiktok.com", "vt.tiktok.com",
        "fb.watch", "redd.it", "instagr.am",
    )

    private val knownHosts: Map<String, SocialPlatform> = mapOf(
        "youtube.com" to SocialPlatform.YOUTUBE,
        "youtu.be" to SocialPlatform.YOUTUBE,
        "music.youtube.com" to SocialPlatform.YOUTUBE,
        "tiktok.com" to SocialPlatform.TIKTOK,
        "vm.tiktok.com" to SocialPlatform.TIKTOK,
        "vt.tiktok.com" to SocialPlatform.TIKTOK,
        "instagram.com" to SocialPlatform.INSTAGRAM,
        "instagr.am" to SocialPlatform.INSTAGRAM,
        "twitter.com" to SocialPlatform.TWITTER_X,
        "x.com" to SocialPlatform.TWITTER_X,
        "t.co" to SocialPlatform.TWITTER_X,
        "facebook.com" to SocialPlatform.FACEBOOK,
        "fb.watch" to SocialPlatform.FACEBOOK,
        "fb.com" to SocialPlatform.FACEBOOK,
        "reddit.com" to SocialPlatform.REDDIT,
        "old.reddit.com" to SocialPlatform.REDDIT,
        "redd.it" to SocialPlatform.REDDIT,
    )

    fun detect(url: String): SocialPlatform {
        val host = hostOf(url) ?: return SocialPlatform.UNKNOWN
        return knownHosts[normalize(host)] ?: SocialPlatform.UNKNOWN
    }

    fun isShortLink(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return normalize(host) in shortenerHosts
    }

    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null
        val authority = afterScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        val hostAndPort = authority.substringAfter('@')
        val host = hostAndPort.substringBefore(':').lowercase()
        return host.ifEmpty { null }
    }

    private fun normalize(host: String): String {
        var normalized = host
        if (normalized.startsWith("www.")) normalized = normalized.removePrefix("www.")
        if (normalized.startsWith("m.")) normalized = normalized.removePrefix("m.")
        return normalized
    }
}
