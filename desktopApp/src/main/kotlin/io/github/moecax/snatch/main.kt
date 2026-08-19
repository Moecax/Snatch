package io.github.moecax.snatch

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.moecax.snatch.di.AppContainer

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Snatch",
    ) {
        App(container = remember { AppContainer() })
    }
}
