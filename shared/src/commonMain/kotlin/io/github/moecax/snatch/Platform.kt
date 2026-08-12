package io.github.moecax.snatch

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform