package io.github.moecax.snatch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

const val LEGAL_NOTICE =
    "Only download content you have the right to save. Respect creators' rights and each platform's terms of service."

@Composable
fun LegalNotice(modifier: Modifier = Modifier) {
    Text(
        text = LEGAL_NOTICE,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
