package com.example.aliveplease.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aliveplease.R
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.utils.EmailContentBuilder
import com.example.aliveplease.utils.TimeFormatter
import com.example.aliveplease.utils.WebhookHelper

data class SettingsUiState(
    val userName: String = "",
    val checkInInterval: String = "",
    val familyInterval: String = "",
    val familyEmail: String = "",
    val familyRecipientTitle: String = "",
    val gasWebhookUrl: String = "",
    val careNotificationEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: String = "",
    val quietHoursEnd: String = "",
    val showSaveMessage: Boolean = false,
    val emailError: Boolean = false,
    val quietHoursError: Boolean = false,
    val tutorialStepIndex: Int = -1
)

class SettingsViewModel(
    private val appContext: Context,
    private val dataStore: AppDataStore
) : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        reloadState(false)
    }

    fun reloadState(tutorialMode: Boolean) {
        val current = dataStore.getFamilyNotifyIntervalFloat()
        uiState = SettingsUiState(
            userName = dataStore.getUserName().takeUnless { it == "??" }.orEmpty(),
            checkInInterval = dataStore.getNotifyInterval().toString(),
            familyInterval = if (current % 1 == 0f) current.toInt().toString() else current.toString(),
            familyEmail = dataStore.getFamilyEmail(),
            familyRecipientTitle = dataStore.getFamilyRecipientTitle()
                .takeUnless { it.contains("??") }
                .orEmpty(),
            gasWebhookUrl = dataStore.getStoredGasWebhookUrl(),
            careNotificationEnabled = dataStore.isCareNotificationOn(),
            quietHoursEnabled = dataStore.isQuietHoursEnabled(),
            quietHoursStart = formatMinutes(dataStore.getQuietHoursStartMinutes()),
            quietHoursEnd = formatMinutes(dataStore.getQuietHoursEndMinutes()),
            tutorialStepIndex = if (tutorialMode) 0 else -1
        )
    }

    fun onUserNameChanged(value: String) {
        uiState = uiState.copy(userName = value)
    }

    fun onCheckInIntervalChanged(value: String) {
        uiState = uiState.copy(checkInInterval = value)
    }

    fun onFamilyIntervalChanged(value: String) {
        uiState = uiState.copy(familyInterval = value)
    }

    fun onFamilyEmailChanged(value: String) {
        uiState = uiState.copy(familyEmail = value, emailError = false)
    }

    fun onFamilyRecipientTitleChanged(value: String) {
        uiState = uiState.copy(familyRecipientTitle = value)
    }

    fun onGasWebhookUrlChanged(value: String) {
        uiState = uiState.copy(gasWebhookUrl = value)
    }

    fun onCareNotificationEnabledChanged(enabled: Boolean) {
        uiState = uiState.copy(careNotificationEnabled = enabled)
    }

    fun onQuietHoursEnabledChanged(enabled: Boolean) {
        uiState = uiState.copy(quietHoursEnabled = enabled, quietHoursError = false)
    }

    fun onQuietHoursStartChanged(value: String) {
        uiState = uiState.copy(quietHoursStart = value, quietHoursError = false)
    }

    fun onQuietHoursEndChanged(value: String) {
        uiState = uiState.copy(quietHoursEnd = value, quietHoursError = false)
    }

    fun onSaveMessageShown() {
        uiState = uiState.copy(showSaveMessage = false)
    }

    fun onTutorialNext(): Boolean {
        if (uiState.tutorialStepIndex < TUTORIAL_LAST_INDEX) {
            uiState = uiState.copy(tutorialStepIndex = uiState.tutorialStepIndex + 1)
            return false
        }
        return true
    }

    fun onTutorialBack(): Boolean {
        if (uiState.tutorialStepIndex > 0) {
            uiState = uiState.copy(tutorialStepIndex = uiState.tutorialStepIndex - 1)
            return false
        }
        return true
    }

    suspend fun sendTestEmail(): String {
        val state = uiState
        if (state.familyEmail.isBlank()) {
            return appContext.getString(R.string.missing_family_email)
        }
        if (!TimeFormatter.isValidEmail(state.familyEmail)) {
            uiState = uiState.copy(emailError = true)
            return appContext.getString(R.string.invalid_email)
        }

        val safeUserName = state.userName.trim().ifBlank { dataStore.getUserName() }
        val safeRecipientTitle = state.familyRecipientTitle.trim()
            .ifBlank { dataStore.getFamilyRecipientTitle() }
        val resolvedWebhookUrl = state.gasWebhookUrl.trim().ifBlank { dataStore.getGasWebhookUrl() }
        val intervalValue = state.familyInterval.toFloatOrNull() ?: dataStore.getFamilyNotifyIntervalFloat()

        val subject = EmailContentBuilder.buildSubject(
            recipientTitle = safeRecipientTitle,
            userName = safeUserName,
            isTest = true
        )
        val body = EmailContentBuilder.buildBody(
            recipientTitle = safeRecipientTitle,
            userName = safeUserName,
            intervalHours = intervalValue,
            isTest = true
        )

        val result = WebhookHelper.sendEmail(
            webhookUrl = resolvedWebhookUrl,
            to = state.familyEmail,
            subject = subject,
            body = body
        )
        return if (result.success) {
            appContext.getString(R.string.test_email_sent)
        } else {
            result.message ?: appContext.getString(R.string.test_email_failed_short)
        }
    }

    fun saveSettings(): Boolean {
        val state = uiState
        if (state.familyEmail.isNotBlank() && !TimeFormatter.isValidEmail(state.familyEmail)) {
            uiState = uiState.copy(emailError = true)
            return false
        }
        val quietStartMinutes = parseTimeToMinutes(state.quietHoursStart)
        val quietEndMinutes = parseTimeToMinutes(state.quietHoursEnd)
        if (quietStartMinutes == null || quietEndMinutes == null) {
            uiState = uiState.copy(quietHoursError = true)
            return false
        }

        state.userName.trim().takeIf { it.isNotBlank() }?.let(dataStore::setUserName)
        state.checkInInterval.toLongOrNull()?.takeIf { it > 0 }?.let(dataStore::setNotifyInterval)
        state.familyInterval.toFloatOrNull()?.takeIf { it > 0 }?.let(dataStore::setFamilyNotifyIntervalFloat)
        dataStore.setFamilyEmail(state.familyEmail)
        dataStore.setFamilyRecipientTitle(state.familyRecipientTitle)
        dataStore.setGasWebhookUrl(state.gasWebhookUrl)
        dataStore.setCareNotificationOn(state.careNotificationEnabled)
        dataStore.setQuietHoursEnabled(state.quietHoursEnabled)
        dataStore.setQuietHoursStartMinutes(quietStartMinutes)
        dataStore.setQuietHoursEndMinutes(quietEndMinutes)

        uiState = uiState.copy(showSaveMessage = true)
        return true
    }

    private fun parseTimeToMinutes(value: String): Int? {
        val trimmed = value.trim()
        if (!Regex("""\d{2}:\d{2}""").matches(trimmed)) return null

        val parts = trimmed.split(":")
        if (parts.size != 2) return null

        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null

        return hour * 60 + minute
    }

    private fun formatMinutes(minutes: Int): String {
        val normalized = minutes.coerceIn(0, 23 * 60 + 59)
        return "%02d:%02d".format(normalized / 60, normalized % 60)
    }

    companion object {
        private const val TUTORIAL_LAST_INDEX = 4

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(
                            appContext,
                            AppDataStore(appContext)
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
