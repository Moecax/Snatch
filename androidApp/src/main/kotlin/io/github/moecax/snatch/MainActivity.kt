package io.github.moecax.snatch

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        enableEdgeToEdge()
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
