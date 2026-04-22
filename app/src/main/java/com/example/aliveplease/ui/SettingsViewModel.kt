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
    val showSaveMessage: Boolean = false,
    val emailError: Boolean = false,
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

        state.userName.trim().takeIf { it.isNotBlank() }?.let(dataStore::setUserName)
        state.checkInInterval.toLongOrNull()?.takeIf { it > 0 }?.let(dataStore::setNotifyInterval)
        state.familyInterval.toFloatOrNull()?.takeIf { it > 0 }?.let(dataStore::setFamilyNotifyIntervalFloat)
        dataStore.setFamilyEmail(state.familyEmail)
        dataStore.setFamilyRecipientTitle(state.familyRecipientTitle)
        dataStore.setGasWebhookUrl(state.gasWebhookUrl)
        dataStore.setCareNotificationOn(state.careNotificationEnabled)

        uiState = uiState.copy(showSaveMessage = true)
        return true
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
