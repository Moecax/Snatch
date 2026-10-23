package io.github.moecax.snatch.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.moecax.snatch.domain.ResolverSettings
import io.github.moecax.snatch.viewmodel.ConnectionTestResult
import io.github.moecax.snatch.viewmodel.SettingsUiState

@Composable
fun SettingsDialog(
    state: SettingsUiState,
    testResult: ConnectionTestResult?,
    onSave: (baseUrl: String, apiKey: String, videoQuality: String) -> Boolean,
    onTestConnection: (baseUrl: String, apiKey: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var baseUrl by remember { mutableStateOf(state.baseUrl) }
    var apiKey by remember { mutableStateOf(state.apiKey) }
    var videoQuality by remember { mutableStateOf(state.videoQuality) }
    var invalidUrl by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text(
                    "Snatch resolves links through a Cobalt server. Use your own instance or a public one " +
                        "that allows unauthenticated use (see instances.cobalt.best).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        invalidUrl = false
                    },
                    label = { Text("Cobalt server URL") },
                    placeholder = { Text("https://cobalt.example.com") },
                    isError = invalidUrl,
                    supportingText = if (invalidUrl) {
                        { Text("Must start with https:// or http://") }
                    } else {
                        null
                    },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                Text("Preferred video quality", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ResolverSettings.QUALITY_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = option == videoQuality,
                            onClick = { videoQuality = option },
                            label = { Text(qualityLabel(option)) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { onTestConnection(baseUrl, apiKey) },
                    enabled = testResult !is ConnectionTestResult.Testing,
                ) {
                    Text("Test connection")
                }
                when (testResult) {
                    ConnectionTestResult.Testing -> Text("Testing…", style = MaterialTheme.typography.bodySmall)
                    is ConnectionTestResult.Success -> Text(
                        "Connected — Cobalt ${testResult.versionLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is ConnectionTestResult.Failure -> Text(
                        testResult.error.toDisplayMessage(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    null -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (onSave(baseUrl, apiKey, videoQuality)) onDismiss() else invalidUrl = true }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun qualityLabel(value: String): String = if (value == "max") "Best available" else "${value}p"
