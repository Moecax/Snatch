package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformDetectorTest {

    private data class Case(val description: String, val url: String, val expected: SocialPlatform)

    private val cases = listOf(
        Case("youtube watch url", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", SocialPlatform.YOUTUBE),
        Case("tiktok video url", "https://www.tiktok.com/@someuser/video/1234567890123456789", SocialPlatform.TIKTOK),
        Case("instagram post url", "https://www.instagram.com/p/Cabcdefghij/", SocialPlatform.INSTAGRAM),
        Case("twitter status url", "https://twitter.com/someuser/status/1234567890123456789", SocialPlatform.TWITTER_X),
        Case("x.com status url", "https://x.com/someuser/status/1234567890123456789", SocialPlatform.TWITTER_X),
        Case("facebook video url", "https://www.facebook.com/someuser/videos/1234567890123456789", SocialPlatform.FACEBOOK),
        Case("reddit comments url", "https://www.reddit.com/r/videos/comments/abc123/some_title/", SocialPlatform.REDDIT),
        Case("youtu.be short link", "https://youtu.be/dQw4w9WgXcQ", SocialPlatform.YOUTUBE),
        Case("vm.tiktok.com short link", "https://vm.tiktok.com/ZMabcdefg/", SocialPlatform.TIKTOK),
        Case("instagr.am short link", "https://instagr.am/p/Cabcdefghij/", SocialPlatform.INSTAGRAM),
        Case("t.co short link", "https://t.co/abcXYZ123", SocialPlatform.TWITTER_X),
        Case("fb.watch short link", "https://fb.watch/abcXYZ123/", SocialPlatform.FACEBOOK),
        Case("redd.it short link", "https://redd.it/abc123", SocialPlatform.REDDIT),
        Case("m. mobile host", "https://m.youtube.com/watch?v=dQw4w9WgXcQ", SocialPlatform.YOUTUBE),
        Case("tracking params", "https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=abc123&feature=share", SocialPlatform.YOUTUBE),
        Case("unrelated host", "https://example.com/some/page", SocialPlatform.UNKNOWN),
        Case("not a url", "not a url at all", SocialPlatform.UNKNOWN),
    )

    @Test
    fun detectsPlatformForEachCase() {
        cases.forEach { case ->
            assertEquals(case.expected, PlatformDetector.detect(case.url), case.description)
        }
    }
}
