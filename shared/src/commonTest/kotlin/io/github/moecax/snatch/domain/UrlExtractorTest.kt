package io.github.moecax.snatch.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlExtractorTest {

    @Test
    fun bareUrlIsReturnedUnchanged() {
        assertEquals("https://youtu.be/abc123", UrlExtractor.firstUrl("https://youtu.be/abc123"))
    }

    @Test
    fun urlEmbeddedInASentenceIsExtracted() {
        assertEquals(
            "https://www.tiktok.com/@user/video/123?is_from_webapp=1",
            UrlExtractor.firstUrl("Check this out! https://www.tiktok.com/@user/video/123?is_from_webapp=1 so funny"),
        )
    }

    @Test
    fun urlOnItsOwnLineAfterATitleIsExtracted() {
        assertEquals(
            "https://x.com/user/status/1",
            UrlExtractor.firstUrl("Some post title\nhttps://x.com/user/status/1\n"),
        )
    }

    @Test
    fun onlyTheFirstUrlIsReturned() {
        assertEquals("https://a.com/1", UrlExtractor.firstUrl("https://a.com/1 and https://b.com/2"))
    }

    @Test
    fun trailingSentencePunctuationIsStripped() {
        assertEquals("https://youtu.be/abc", UrlExtractor.firstUrl("Watch https://youtu.be/abc."))
        assertEquals("https://youtu.be/abc", UrlExtractor.firstUrl("(see https://youtu.be/abc)"))
        assertEquals("https://youtu.be/abc", UrlExtractor.firstUrl("\"https://youtu.be/abc\","))
    }

    @Test
    fun balancedParenthesisInsideUrlIsKept() {
        assertEquals(
            "https://en.wikipedia.org/wiki/Snatch_(film)",
            UrlExtractor.firstUrl("https://en.wikipedia.org/wiki/Snatch_(film)"),
        )
    }

    @Test
    fun textWithoutAUrlReturnsNull() {
        assertNull(UrlExtractor.firstUrl("no link here"))
        assertNull(UrlExtractor.firstUrl(""))
    }

    @Test
    fun schemeOnlyIsNotAUrl() {
        assertNull(UrlExtractor.firstUrl("broken https:// link"))
    }
}
