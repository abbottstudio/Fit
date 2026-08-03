package com.fitcoachpro.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachpro.app.data.PrefsRepository
import com.fitcoachpro.app.notifications.ReminderScheduler
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = PrefsRepository(application)

    var backendUrl by mutableStateOf("")
    var sharedSecret by mutableStateOf("")
    var reminderHour by mutableStateOf(7)
    var reminderMinute by mutableStateOf(0)
    var reminderEnabled by mutableStateOf(false)
    var savedConfirmation by mutableStateOf(false)

    init {
        viewModelScope.launch {
            backendUrl = prefsRepository.snapshotBackendUrl()
            sharedSecret = prefsRepository.snapshotSharedSecret()
            reminderHour = prefsRepository.snapshotReminderHour()
            reminderMinute = prefsRepository.snapshotReminderMinute()
            reminderEnabled = prefsRepository.snapshotReminderEnabled()
        }
    }

    fun save() {
        viewModelScope.launch {
            prefsRepository.saveBackendConfig(backendUrl, sharedSecret)
            prefsRepository.saveReminder(reminderHour, reminderMinute, reminderEnabled)

            val app = getApplication<Application>()
            if (reminderEnabled) {
                ReminderScheduler.schedule(app, reminderHour, reminderMinute)
            } else {
                ReminderScheduler.cancel(app)
            }

            savedConfirmation = true
        }
    }
}
