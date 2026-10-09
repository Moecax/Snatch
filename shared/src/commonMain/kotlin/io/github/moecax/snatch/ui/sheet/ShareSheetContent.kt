package io.github.moecax.snatch.ui.sheet

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.ui.LegalNotice
import io.github.moecax.snatch.ui.MediaThumbnail
import io.github.moecax.snatch.ui.toDisplayMessage
import io.github.moecax.snatch.viewmodel.DownloadEvent
import io.github.moecax.snatch.viewmodel.DownloadUiState

@Composable
fun ShareSheetContent(
    state: DownloadUiState,
    onEvent: (DownloadEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            DownloadUiState.Idle, is DownloadUiState.Resolving -> SheetSkeleton()
            is DownloadUiState.Ready -> ReadySheet(state, onEvent)
            is DownloadUiState.Downloading -> DownloadingSheet(state)
            is DownloadUiState.Complete -> CompleteSheet(state, onDismiss)
            is DownloadUiState.Failed -> FailedSheet(state, onEvent, onDismiss)
        }
        LegalNotice()
    }
}

@Composable
private fun ReadySheet(state: DownloadUiState.Ready, onEvent: (DownloadEvent) -> Unit) {
    MediaHeader(state.media)
    if (state.media.variants.size > 1) {
        QualityChips(
            variants = state.media.variants.map { it.id to it.label },
            selectedId = state.selectedVariantId,
            onSelected = { onEvent(DownloadEvent.VariantSelected(it)) },
        )
    }
    Button(onClick = { onEvent(DownloadEvent.DownloadClicked) }, modifier = Modifier.fillMaxWidth()) {
        Text("Download")
    }
}

@Composable
private fun MediaHeader(media: ResolvedMedia) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MediaThumbnail(url = media.thumbnailUrl, size = 72.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(media.title ?: "Untitled", style = MaterialTheme.typography.titleMedium, maxLines = 2)
            media.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
        }
    }
}

@Composable
private fun QualityChips(variants: List<Pair<String, String>>, selectedId: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        variants.forEach { (id, label) ->
            FilterChip(
                selected = id == selectedId,
                onClick = { onSelected(id) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun DownloadingSheet(state: DownloadUiState.Downloading) {
    MediaHeader(state.media)
    Text("Downloading ${state.variant.label}…", style = MaterialTheme.typography.bodyMedium)
    val total = state.totalBytes
    if (total != null && total > 0) {
        LinearProgressIndicator(
            progress = { state.bytesDownloaded.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompleteSheet(state: DownloadUiState.Complete, onDismiss: () -> Unit) {
    Text("Saved ${state.fileName}", style = MaterialTheme.typography.titleMedium)
    Text(state.location, style = MaterialTheme.typography.bodySmall)
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun FailedSheet(state: DownloadUiState.Failed, onEvent: (DownloadEvent) -> Unit, onDismiss: () -> Unit) {
    Text(state.error.toDisplayMessage(), color = MaterialTheme.colorScheme.error)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.retryable) {
            Button(onClick = { onEvent(DownloadEvent.Retry) }) { Text("Retry") }
        }
        TextButton(onClick = onDismiss) { Text("Close") }
    }
}

@Composable
private fun SheetSkeleton() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
    )
    val color = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.alpha(alpha)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(72.dp).background(color, RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.fillMaxWidth(0.7f).height(16.dp).background(color, RoundedCornerShape(4.dp)))
                Spacer(Modifier.fillMaxWidth(0.4f).height(12.dp).background(color, RoundedCornerShape(4.dp)))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Spacer(Modifier.size(width = 72.dp, height = 32.dp).background(color, RoundedCornerShape(8.dp))) }
        }
        Spacer(Modifier.fillMaxWidth().height(40.dp).background(color, RoundedCornerShape(20.dp)))
    }
}
