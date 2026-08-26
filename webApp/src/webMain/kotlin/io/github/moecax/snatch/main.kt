package io.github.moecax.snatch

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.moecax.snatch.di.AppContainer

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(container = remember { AppContainer() })
    }
}
