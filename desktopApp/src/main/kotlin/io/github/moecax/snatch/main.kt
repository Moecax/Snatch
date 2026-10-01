package io.github.moecax.snatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.moecax.snatch.data.DesktopFileSink
import io.github.moecax.snatch.data.DesktopSettings
import io.github.moecax.snatch.di.AppContainer
import java.awt.Dimension
import java.io.File
import javax.swing.JFileChooser
import org.jetbrains.skia.Image

private val MinWindowSize = Dimension(420, 480)

fun main() = application {
    val settings = remember { DesktopSettings() }
    var downloadDirectory by remember { mutableStateOf(settings.downloadDirectory) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Snatch",
        icon = remember { appIcon() },
        state = rememberWindowState(size = DpSize(560.dp, 640.dp)),
    ) {
        window.minimumSize = MinWindowSize

        DownloadFolderMenu(
            currentDirectory = downloadDirectory,
            onChange = { picked ->
                settings.downloadDirectory = picked
                downloadDirectory = picked
            },
        )

        App(container = remember { AppContainer(DesktopFileSink(settings), settingsStore = settings) })
    }
}

private fun appIcon(): BitmapPainter {
    val bytes = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")).use { it.readBytes() }
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

@Composable
private fun FrameWindowScope.DownloadFolderMenu(
    currentDirectory: File,
    onChange: (File) -> Unit,
) {
    MenuBar {
        Menu("Settings") {
            Item("Download folder: ${currentDirectory.absolutePath}", enabled = false, onClick = {})
            Item("Change download folder…", onClick = {
                val chooser = JFileChooser(currentDirectory).apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Choose download folder"
                }
                if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    onChange(chooser.selectedFile)
                }
            })
        }
    }
}
