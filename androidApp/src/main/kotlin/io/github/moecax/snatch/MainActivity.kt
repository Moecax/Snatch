package io.github.moecax.snatch

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.github.moecax.snatch.data.AndroidFileSink
import io.github.moecax.snatch.di.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark-only, so system bar icons must stay light even when the system theme is light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            App(
                container = (application as SnatchApplication).container,
                interceptEvents = { onEvent -> rememberDownloadGate(onEvent) },
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    App(container = remember { AppContainer(AndroidFileSink(context)) })
}
