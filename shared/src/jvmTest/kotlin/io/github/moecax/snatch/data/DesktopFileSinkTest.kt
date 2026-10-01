package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.model.MediaType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopFileSinkTest {

    private val root: File = Files.createTempDirectory("snatch-test").toFile()
    private val settings = DesktopSettings(File(root, "config/settings.properties"))
    private val sink = DesktopFileSink(settings)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun writesIntoConfiguredDownloadDirectory() = runTest {
        val target = File(root, "custom/nested")
        settings.downloadDirectory = target

        val path = sink.write("clip.mp4", "video/mp4", MediaType.Video, flowOf(byteArrayOf(1, 2, 3)))

        assertEquals(File(target, "clip.mp4").absolutePath, path)
        assertTrue(File(path).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun changingDirectoryAppliesToNextWrite() = runTest {
        val first = File(root, "first")
        val second = File(root, "second")

        settings.downloadDirectory = first
        sink.write("a.bin", "application/octet-stream", MediaType.Video, flowOf(byteArrayOf(1)))
        settings.downloadDirectory = second
        sink.write("b.bin", "application/octet-stream", MediaType.Video, flowOf(byteArrayOf(2)))

        assertTrue(File(first, "a.bin").exists())
        assertTrue(File(second, "b.bin").exists())
        assertFalse(File(first, "b.bin").exists())
    }

    @Test
    fun settingPersistsAcrossInstances() {
        val target = File(root, "persisted")
        settings.downloadDirectory = target

        assertEquals(target.absoluteFile, DesktopSettings(File(root, "config/settings.properties")).downloadDirectory)
    }

    @Test
    fun defaultsToUserDownloadsWhenUnset() {
        assertEquals(File(System.getProperty("user.home"), "Downloads"), settings.downloadDirectory)
    }
}
