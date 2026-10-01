import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "io.github.moecax.snatch.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Snatch"
            packageVersion = "0.1.0"
            description = "Social media downloader"

            // The jlink-trimmed runtime only contains modules the Compose plugin can infer; Ktor's CIO
            // engine needs the network-facing ones, so a packaged app can fail at runtime on a
            // download even though `run` works. Listing them here is cheaper than debugging that.
            modules("java.net.http", "jdk.unsupported")

            windows {
                menuGroup = "Snatch"
                // Must stay constant across releases so a newer MSI upgrades an existing install
                // instead of installing side by side.
                upgradeUuid = "5d0b4a1e-7c3f-4c6e-9f1a-2b8e6d4f0a71"
                shortcut = true
                dirChooser = true
                iconFile.set(project.file("icons/icon.ico"))
            }
        }
    }
}