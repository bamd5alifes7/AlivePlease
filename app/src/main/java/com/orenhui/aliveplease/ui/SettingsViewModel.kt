package com.orenhui.aliveplease.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orenhui.aliveplease.R
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.utils.EmailContentBuilder
import com.orenhui.aliveplease.utils.TimeFormatter
import com.orenhui.aliveplease.utils.WebhookHelper

data class SettingsUiState(
    val userName: String = "",
    val checkInInterval: String = "",
    val familyInterval: String = "",
    val familyWarningBefore: String = "",
    val familyEmail: String = "",
    val familyRecipientTitle: String = "",
    val gasWebhookUrl: String = "",
    val careNotificationEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: String = "",
    val quietHoursEnd: String = "",
    val emailError: Boolean = false,
    val checkInIntervalError: Boolean = false,
    val familyIntervalError: Boolean = false,
    val familyWarningError: Boolean = false,
    val quietHoursError: Boolean = false,
    val tutorialStepIndex: Int = -1
)

class SettingsViewModel(
    private val appContext: Context,
    private val dataStore: AppDataStore,
    initialTutorialMode: Boolean = false,
    initialTutorialStartIndex: Int = 0
) : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        reloadState(initialTutorialMode, initialTutorialStartIndex)
    }

    fun reloadState(tutorialMode: Boolean, tutorialStartIndex: Int = 0) {
        val current = dataStore.getFamilyNotifyIntervalFloat()
        uiState = SettingsUiState(
            userName = dataStore.getUserName(),
            checkInInterval = dataStore.getNotifyInterval().toString(),
            familyInterval = if (current % 1 == 0f) current.toInt().toString() else current.toString(),
            familyWarningBefore = formatHours(dataStore.getFamilyWarningBeforeHours()),
            familyEmail = dataStore.getFamilyEmail(),
            familyRecipientTitle = dataStore.getFamilyRecipientTitle(),
            gasWebhookUrl = dataStore.getStoredGasWebhookUrl(),
            careNotificationEnabled = dataStore.isCareNotificationOn(),
            quietHoursEnabled = dataStore.isQuietHoursEnabled(),
            quietHoursStart = formatMinutes(dataStore.getQuietHoursStartMinutes()),
            quietHoursEnd = formatMinutes(dataStore.getQuietHoursEndMinutes()),
            tutorialStepIndex = if (tutorialMode) tutorialStartIndex else -1
        )
    }

    fun onUserNameChanged(value: String) {
        uiState = uiState.copy(userName = value)
    }

    fun onCheckInIntervalChanged(value: String) {
        uiState = uiState.copy(
            checkInInterval = sanitizeIntegerInput(value),
            checkInIntervalError = false
        )
    }

    fun onFamilyIntervalChanged(value: String) {
        uiState = uiState.copy(
            familyInterval = sanitizeDecimalInput(value),
            familyIntervalError = false,
            familyWarningError = false
        )
    }

    fun onFamilyWarningBeforeChanged(value: String) {
        uiState = uiState.copy(
            familyWarningBefore = sanitizeDecimalInput(value),
            familyWarningError = false
        )
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

    fun onTutorialNext(lastIndex: Int): Boolean {
        if (uiState.tutorialStepIndex < lastIndex) {
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

    fun hasUnsavedChanges(): Boolean {
        val state = uiState
        return state.userName.trim().ifBlank { dataStore.getUserName() } != dataStore.getUserName() ||
            state.checkInInterval.trim() != dataStore.getNotifyInterval().toString() ||
            state.familyInterval.trim() != formatHours(dataStore.getFamilyNotifyIntervalFloat()) ||
            state.familyWarningBefore.trim() != formatHours(dataStore.getFamilyWarningBeforeHours()) ||
            state.familyEmail.trim() != dataStore.getFamilyEmail() ||
            state.familyRecipientTitle.trim().ifBlank { dataStore.getFamilyRecipientTitle() } !=
            dataStore.getFamilyRecipientTitle() ||
            state.gasWebhookUrl.trim() != dataStore.getStoredGasWebhookUrl() ||
            state.careNotificationEnabled != dataStore.isCareNotificationOn() ||
            state.quietHoursEnabled != dataStore.isQuietHoursEnabled() ||
            state.quietHoursStart.trim() != formatMinutes(dataStore.getQuietHoursStartMinutes()) ||
            state.quietHoursEnd.trim() != formatMinutes(dataStore.getQuietHoursEndMinutes())
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
        val intervalValue = state.familyInterval.toFloatOrNull()
            ?.takeIf { it >= MIN_DECIMAL_INTERVAL_HOURS && it <= MAX_DECIMAL_INTERVAL_HOURS }
            ?: dataStore.getFamilyNotifyIntervalFloat()

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
        val checkInIntervalValue = state.checkInInterval.toLongOrNull()
        if (checkInIntervalValue == null || checkInIntervalValue !in 1L..MAX_INTERVAL_HOURS) {
            uiState = uiState.copy(checkInIntervalError = true)
            return false
        }
        val familyIntervalValue = state.familyInterval.toFloatOrNull()
        if (
            familyIntervalValue == null ||
            familyIntervalValue < MIN_DECIMAL_INTERVAL_HOURS ||
            familyIntervalValue > MAX_DECIMAL_INTERVAL_HOURS
        ) {
            uiState = uiState.copy(familyIntervalError = true)
            return false
        }
        val familyWarningBeforeValue = state.familyWarningBefore.toFloatOrNull()
        if (
            familyWarningBeforeValue == null ||
            familyWarningBeforeValue < MIN_DECIMAL_INTERVAL_HOURS ||
            familyWarningBeforeValue >= familyIntervalValue
        ) {
            uiState = uiState.copy(familyWarningError = true)
            return false
        }

        state.userName.trim().takeIf { it.isNotBlank() }?.let(dataStore::setUserName)
        dataStore.setNotifyInterval(checkInIntervalValue)
        dataStore.setFamilyNotifyIntervalFloat(familyIntervalValue)
        dataStore.setFamilyWarningBeforeHours(familyWarningBeforeValue)
        dataStore.setFamilyEmail(state.familyEmail)
        dataStore.setFamilyRecipientTitle(state.familyRecipientTitle)
        dataStore.setGasWebhookUrl(state.gasWebhookUrl)
        dataStore.setCareNotificationOn(state.careNotificationEnabled)
        dataStore.setQuietHoursEnabled(state.quietHoursEnabled)
        dataStore.setQuietHoursStartMinutes(quietStartMinutes)
        dataStore.setQuietHoursEndMinutes(quietEndMinutes)

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

    private fun formatHours(hours: Float): String {
        return if (hours % 1 == 0f) hours.toInt().toString() else hours.toString()
    }

    private fun sanitizeIntegerInput(value: String): String {
        return value.filter(Char::isDigit).take(3)
    }

    private fun sanitizeDecimalInput(value: String): String {
        val builder = StringBuilder()
        var hasDecimalPoint = false
        var wholeDigits = 0
        var decimalDigits = 0

        value.forEach { char ->
            when {
                char.isDigit() && !hasDecimalPoint && wholeDigits < 3 -> {
                    builder.append(char)
                    wholeDigits++
                }
                char == '.' && !hasDecimalPoint && builder.isNotEmpty() -> {
                    builder.append(char)
                    hasDecimalPoint = true
                }
                char.isDigit() && hasDecimalPoint && decimalDigits < MAX_DECIMAL_DIGITS -> {
                    builder.append(char)
                    decimalDigits++
                }
            }
        }

        return builder.toString()
    }

    companion object {
        private const val MAX_INTERVAL_HOURS = 240L
        private const val MIN_DECIMAL_INTERVAL_HOURS = 0.01f
        private const val MAX_DECIMAL_INTERVAL_HOURS = 240f
        private const val MAX_DECIMAL_DIGITS = 2

        fun factory(
            context: Context,
            initialTutorialMode: Boolean = false,
            initialTutorialStartIndex: Int = 0
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(
                            appContext,
                            AppDataStore(appContext),
                            initialTutorialMode,
                            initialTutorialStartIndex
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
