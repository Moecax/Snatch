package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.MediaResolver
import io.github.moecax.snatch.domain.MediaResolverContractTest
import io.github.moecax.snatch.domain.model.SocialPlatform

class FakeResolverContractTest : MediaResolverContractTest() {
    override val resolver: MediaResolver = FakeResolver()
    override val supportedPlatform = SocialPlatform.YOUTUBE
    override val supportedUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    override val unsupportedPlatform = SocialPlatform.TIKTOK
    override val unsupportedUrl = "https://www.tiktok.com/@someuser/video/1234567890123456789"
}
