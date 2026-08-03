package com.fitcoachpro.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Backend URL + shared secret + reminder time, all user-editable at runtime.
 * Deliberately not hardcoded - see BackendApi.kt's note on why backend
 * hosting is an open decision this app shouldn't assume.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.backendUrl,
                onValueChange = {
                    viewModel.backendUrl = it
                    viewModel.savedConfirmation = false
                },
                label = { Text("Backend URL (e.g. https://your-backend.example.com/)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            val trimmedUrl = viewModel.backendUrl.trim()
            if (trimmedUrl.isNotEmpty() && !trimmedUrl.startsWith("https://", ignoreCase = true)) {
                val warning = if (trimmedUrl.startsWith("http://", ignoreCase = true)) {
                    // Release builds block all cleartext HTTP outright (see
                    // network_security_config.xml) - a plain http:// URL here
                    // will just fail with a generic "couldn't reach backend"
                    // error and no explanation, so spell it out up front.
                    "http:// only works in debug builds on your local network " +
                        "(see network_security_config_debug.xml). A release build " +
                        "needs https:// or every request will be blocked."
                } else {
                    // No scheme at all (e.g. "192.168.1.5:3000") - Retrofit
                    // rejects this at request time with a fairly opaque
                    // IllegalArgumentException, surfaced to you as "Couldn't
                    // reach backend: ..." Catching it here is a clearer signal.
                    "Needs a scheme - start it with http:// or https://."
                }
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            OutlinedTextField(
                value = viewModel.sharedSecret,
                onValueChange = {
                    viewModel.sharedSecret = it
                    viewModel.savedConfirmation = false
                },
                label = { Text("API shared secret (from backend .env)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Daily check-in reminder")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled")
                Switch(
                    checked = viewModel.reminderEnabled,
                    onCheckedChange = {
                        viewModel.reminderEnabled = it
                        viewModel.savedConfirmation = false
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.reminderHour.toString(),
                    onValueChange = {
                        viewModel.reminderHour = it.toIntOrNull()?.coerceIn(0, 23) ?: viewModel.reminderHour
                        viewModel.savedConfirmation = false
                    },
                    label = { Text("Hour (0-23)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.reminderMinute.toString(),
                    onValueChange = {
                        viewModel.reminderMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: viewModel.reminderMinute
                        viewModel.savedConfirmation = false
                    },
                    label = { Text("Minute (0-59)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            // Stays visible after saving (does NOT auto-navigate back) so
            // there's actual on-screen confirmation the save took effect,
            // rather than popping back to check-in before you can see it.
            if (viewModel.savedConfirmation) {
                Text(
                    text = "Saved.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
