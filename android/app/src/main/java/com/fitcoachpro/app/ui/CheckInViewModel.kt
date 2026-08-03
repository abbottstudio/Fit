package com.fitcoachpro.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachpro.app.data.ApiErrorBody
import com.fitcoachpro.app.data.BackendApiClient
import com.fitcoachpro.app.data.CheckInRequest
import com.fitcoachpro.app.data.PrefsRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class CheckInUiState {
    object Idle : CheckInUiState()
    object Loading : CheckInUiState()
    data class Success(val reply: String) : CheckInUiState()
    data class Error(val message: String) : CheckInUiState()
}

/**
 * Holds the check-in form fields and submits them to POST /checkin. Field
 * set matches AGENT_PROMPT.md's "Daily Check-In" section exactly (weight,
 * sleep, energy/stress/motivation 1-10, soreness, joint pain, hydration,
 * protein, steps, ready to train) - see ../../CLAUDE.md / IMPLEMENTATION_STEPS.md
 * Phase 1 for why these specific fields.
 */
class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = PrefsRepository(application)

    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    var weightKg by mutableStateOf("")
    var sleepHours by mutableStateOf("")
    var energy by mutableStateOf("")
    var stress by mutableStateOf("")
    var motivation by mutableStateOf("")
    var soreness by mutableStateOf("")
    var jointPain by mutableStateOf("")
    var hydrationL by mutableStateOf("")
    var proteinG by mutableStateOf("")
    var steps by mutableStateOf("")
    var readyToTrain by mutableStateOf(true)
    var message by mutableStateOf("")

    fun submit() {
        viewModelScope.launch {
            val backendUrl = prefsRepository.snapshotBackendUrl()
            val sharedSecret = prefsRepository.snapshotSharedSecret()

            if (backendUrl.isBlank() || sharedSecret.isBlank()) {
                _uiState.value = CheckInUiState.Error(
                    "Set your backend URL and shared secret in Settings first."
                )
                return@launch
            }

            _uiState.value = CheckInUiState.Loading

            // Fixed to Asia/Kolkata, not the device's local timezone - matches
            // the backend's canonical timezone (see backend/src/db.js's
            // default config.timezone). Using the device's local zone would
            // log the wrong calendar date around midnight if you're
            // travelling outside IST.
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(Date())
            val request = CheckInRequest(
                date = today,
                weightKg = weightKg.toDoubleOrNull(),
                sleepHours = sleepHours.toDoubleOrNull(),
                energy = energy.toIntOrNull(),
                stress = stress.toIntOrNull(),
                motivation = motivation.toIntOrNull(),
                soreness = soreness.ifBlank { null },
                jointPain = jointPain.ifBlank { null },
                hydrationL = hydrationL.toDoubleOrNull(),
                proteinG = proteinG.toDoubleOrNull(),
                steps = steps.toIntOrNull(),
                readyToTrain = readyToTrain,
                message = message.ifBlank { null }
            )

            try {
                val api = BackendApiClient.create(backendUrl, sharedSecret)
                val response = api.checkIn(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.value = CheckInUiState.Success(body?.reply ?: "(empty response)")
                } else {
                    val errorText = response.errorBody()?.string()
                    val parsed = try {
                        Gson().fromJson(errorText, ApiErrorBody::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    val detail = parsed?.detail ?: parsed?.error ?: errorText ?: "HTTP ${response.code()}"
                    _uiState.value = CheckInUiState.Error("Backend error: $detail")
                }
            } catch (e: Exception) {
                _uiState.value = CheckInUiState.Error("Couldn't reach backend: ${e.message}")
            }
        }
    }
}
