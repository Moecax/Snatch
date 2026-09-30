package io.github.moecax.snatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.moecax.snatch.data.AndroidFileSink
import io.github.moecax.snatch.di.AppContainer
import io.github.moecax.snatch.domain.UrlExtractor
import io.github.moecax.snatch.ui.sheet.ShareSheetContent
import io.github.moecax.snatch.viewmodel.DownloadEvent
import io.github.moecax.snatch.viewmodel.DownloadViewModel

class ShareActivity : ComponentActivity() {

    private var sharedText by mutableStateOf("")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText = extractSharedText(intent)

        setContent {
            val container = remember { AppContainer(AndroidFileSink(applicationContext)) }
            MaterialTheme {
                val viewModel: DownloadViewModel = viewModel { container.createDownloadViewModel() }
                val state by viewModel.state.collectAsStateWithLifecycle()

                // Survives rotation so the same share isn't resubmitted; a new share arriving via
                // onNewIntent changes sharedText and does resubmit.
                var submittedText by rememberSaveable { mutableStateOf<String?>(null) }
                LaunchedEffect(sharedText) {
                    if (submittedText != sharedText) {
                        submittedText = sharedText
                        viewModel.onEvent(DownloadEvent.UrlSubmitted(UrlExtractor.firstUrl(sharedText) ?: sharedText))
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = ::finish,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    ShareSheetContent(state = state, onEvent = viewModel::onEvent, onDismiss = ::finish)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = extractSharedText(intent)
    }

    private fun extractSharedText(intent: Intent): String =
        intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
}
