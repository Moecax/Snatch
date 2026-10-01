package io.github.moecax.snatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.viewmodel.DownloadEvent
import io.github.moecax.snatch.viewmodel.DownloadUiState

@Composable
fun MainScreen(state: DownloadUiState, onEvent: (DownloadEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeContentPadding().padding(16.dp)) {
        UrlInput(state = state, onEvent = onEvent)
        Spacer(Modifier.height(16.dp))
        when (state) {
            DownloadUiState.Idle -> Unit
            is DownloadUiState.Resolving -> ResolvingIndicator()
            is DownloadUiState.Ready -> ReadyContent(state, onEvent)
            is DownloadUiState.Downloading -> DownloadingContent(state)
            is DownloadUiState.Complete -> CompleteContent(state, onEvent)
            is DownloadUiState.Failed -> FailedContent(state, onEvent)
        }
        Spacer(Modifier.weight(1f))
        LegalNotice()
    }
}

@Composable
private fun UrlInput(state: DownloadUiState, onEvent: (DownloadEvent) -> Unit) {
    var url by remember { mutableStateOf("") }
    val busy = state is DownloadUiState.Resolving || state is DownloadUiState.Downloading
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Paste a link") },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { onEvent(DownloadEvent.UrlSubmitted(url)) },
            enabled = url.isNotBlank() && !busy,
        ) {
            Text("Go")
        }
    }
}

@Composable
private fun ResolvingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Resolving…")
    }
}

@Composable
private fun ReadyContent(state: DownloadUiState.Ready, onEvent: (DownloadEvent) -> Unit) {
    Column {
        MediaCard(state.media)
        if (state.media.variants.size > 1) {
            Spacer(Modifier.height(8.dp))
            QualityPicker(
                variants = state.media.variants,
                selectedVariantId = state.selectedVariantId,
                onSelected = { onEvent(DownloadEvent.VariantSelected(it)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onEvent(DownloadEvent.DownloadClicked) }) {
            Text("Download")
        }
    }
}

@Composable
private fun MediaCard(media: ResolvedMedia) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(media.title ?: "Untitled", style = MaterialTheme.typography.titleMedium)
            media.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun QualityPicker(variants: List<MediaVariant>, selectedVariantId: String, onSelected: (String) -> Unit) {
    Column {
        variants.forEach { variant ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = variant.id == selectedVariantId,
                        onClick = { onSelected(variant.id) },
                    ),
            ) {
                RadioButton(selected = variant.id == selectedVariantId, onClick = { onSelected(variant.id) })
                Text(variant.label)
            }
        }
    }
}

@Composable
private fun DownloadingContent(state: DownloadUiState.Downloading) {
    Column {
        Text("Downloading ${state.variant.label}…")
        val total = state.totalBytes
        if (total != null && total > 0) {
            LinearProgressIndicator(progress = { state.bytesDownloaded.toFloat() / total.toFloat() })
        } else {
            LinearProgressIndicator()
        }
    }
}

@Composable
private fun CompleteContent(state: DownloadUiState.Complete, onEvent: (DownloadEvent) -> Unit) {
    Column {
        Text("Saved ${state.fileName} to ${state.location}")
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onEvent(DownloadEvent.Reset) }) {
            Text("Download another")
        }
    }
}

@Composable
private fun FailedContent(state: DownloadUiState.Failed, onEvent: (DownloadEvent) -> Unit) {
    Column {
        Text(state.error.toDisplayMessage(), color = MaterialTheme.colorScheme.error)
        if (state.retryable) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onEvent(DownloadEvent.Retry) }) {
                Text("Retry")
            }
        }
    }
}
