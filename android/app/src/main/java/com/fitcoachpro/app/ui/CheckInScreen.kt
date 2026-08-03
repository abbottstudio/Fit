package com.fitcoachpro.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Phase 1 core loop screen: fill the daily check-in form (fields per
 * AGENT_PROMPT.md's "Daily Check-In" section), submit to the backend, show
 * the coach's reply. This is the app's home screen - opened directly on
 * launch and by the reminder notification's tap action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    onOpenSettings: () -> Unit,
    viewModel: CheckInViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's check-in") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Text("⚙️") // gear glyph - avoids pulling in the
                        // material-icons-core/extended artifact for one icon
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.weightKg,
                onValueChange = { viewModel.weightKg = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.sleepHours,
                onValueChange = { viewModel.sleepHours = it },
                label = { Text("Sleep last night (hours)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.energy,
                onValueChange = { viewModel.energy = it },
                label = { Text("Energy (1-10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.stress,
                onValueChange = { viewModel.stress = it },
                label = { Text("Stress (1-10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.motivation,
                onValueChange = { viewModel.motivation = it },
                label = { Text("Motivation (1-10)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.soreness,
                onValueChange = { viewModel.soreness = it },
                label = { Text("Muscle soreness (e.g. none/mild/moderate/severe)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.jointPain,
                onValueChange = { viewModel.jointPain = it },
                label = { Text("Joint pain (e.g. none, or describe)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.hydrationL,
                onValueChange = { viewModel.hydrationL = it },
                label = { Text("Hydration yesterday (L)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.proteinG,
                onValueChange = { viewModel.proteinG = it },
                label = { Text("Protein yesterday (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.steps,
                onValueChange = { viewModel.steps = it },
                label = { Text("Steps yesterday") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Ready to train today?")
                Checkbox(
                    checked = viewModel.readyToTrain,
                    onCheckedChange = { viewModel.readyToTrain = it }
                )
            }

            OutlinedTextField(
                value = viewModel.message,
                onValueChange = { viewModel.message = it },
                label = { Text("Anything else for your coach? (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is CheckInUiState.Loading
            ) {
                Text("Submit check-in")
            }

            when (val state = uiState) {
                is CheckInUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text("Waiting for your coach's response…")
                    }
                }
                is CheckInUiState.Success -> {
                    Text(
                        text = state.reply,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is CheckInUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is CheckInUiState.Idle -> {
                    // Nothing to show yet.
                }
            }
        }
    }
}
