package com.orenhui.aliveplease.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orenhui.aliveplease.R
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.data.FamilyContact
import com.orenhui.aliveplease.utils.EmailContentBuilder
import com.orenhui.aliveplease.utils.TimeFormatter
import com.orenhui.aliveplease.utils.WebhookHelper

data class SettingsUiState(
    val userName: String = "",
    val checkInInterval: String = "",
    val familyInterval: String = "",
    val familyWarningBefore: String = "",
    val familyContacts: List<FamilyContactUiState> = listOf(FamilyContactUiState()),
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
    val tutorialStepIndex: Int = -1,
    // 生日設定
    val birthdayMonth: String = "",   // 空字串表示未設定
    val birthdayDay: String = "",     // 空字串表示未設定
    val birthdayError: Boolean = false
)

data class FamilyContactUiState(
    val recipientTitle: String = "",
    val email: String = ""
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
        val storedMonth = dataStore.getBirthdayMonth()
        val storedDay = dataStore.getBirthdayDay()
        uiState = SettingsUiState(
            userName = dataStore.getUserName(),
            checkInInterval = dataStore.getNotifyInterval().toString(),
            familyInterval = if (current % 1 == 0f) current.toInt().toString() else current.toString(),
            familyWarningBefore = formatHours(dataStore.getFamilyWarningBeforeHours()),
            familyContacts = dataStore.getFamilyContacts()
                .map { FamilyContactUiState(it.recipientTitle, it.email) }
                .ifEmpty { listOf(FamilyContactUiState()) },
            gasWebhookUrl = dataStore.getStoredGasWebhookUrl(),
            careNotificationEnabled = dataStore.isCareNotificationOn(),
            quietHoursEnabled = dataStore.isQuietHoursEnabled(),
            quietHoursStart = formatMinutes(dataStore.getQuietHoursStartMinutes()),
            quietHoursEnd = formatMinutes(dataStore.getQuietHoursEndMinutes()),
            tutorialStepIndex = if (tutorialMode) tutorialStartIndex else -1,
            birthdayMonth = if (storedMonth == 0) "" else storedMonth.toString(),
            birthdayDay = if (storedDay == 0) "" else storedDay.toString()
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

    fun onFamilyContactTitleChanged(index: Int, value: String) {
        updateFamilyContact(index) { it.copy(recipientTitle = value) }
    }

    fun onFamilyContactEmailChanged(index: Int, value: String) {
        updateFamilyContact(index) { it.copy(email = value) }
    }

    fun addFamilyContact() {
        uiState = uiState.copy(
            familyContacts = uiState.familyContacts + FamilyContactUiState(),
            emailError = false
        )
    }

    fun removeFamilyContact(index: Int) {
        val contacts = uiState.familyContacts.toMutableList()
        if (index !in contacts.indices) return
        contacts.removeAt(index)
        uiState = uiState.copy(
            familyContacts = contacts.ifEmpty { mutableListOf(FamilyContactUiState()) },
            emailError = false
        )
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

    fun onBirthdayMonthChanged(value: String) {
        val sanitized = value.filter(Char::isDigit).take(2)
        uiState = uiState.copy(birthdayMonth = sanitized, birthdayError = false)
    }

    fun onBirthdayDayChanged(value: String) {
        val sanitized = value.filter(Char::isDigit).take(2)
        uiState = uiState.copy(birthdayDay = sanitized, birthdayError = false)
    }

    fun clearBirthday() {
        dataStore.clearBirthday()
        uiState = uiState.copy(birthdayMonth = "", birthdayDay = "", birthdayError = false)
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
            normalizedFamilyContacts(state.familyContacts) != dataStore.getFamilyContacts() ||
            state.gasWebhookUrl.trim() != dataStore.getStoredGasWebhookUrl() ||
            state.careNotificationEnabled != dataStore.isCareNotificationOn() ||
            state.quietHoursEnabled != dataStore.isQuietHoursEnabled() ||
            state.quietHoursStart.trim() != formatMinutes(dataStore.getQuietHoursStartMinutes()) ||
            state.quietHoursEnd.trim() != formatMinutes(dataStore.getQuietHoursEndMinutes())
    }

    suspend fun sendTestEmail(): String {
        val state = uiState
        val contacts = validateFamilyContactsOrNull(state.familyContacts)
        if (contacts == null) {
            uiState = uiState.copy(emailError = true)
            return appContext.getString(R.string.invalid_family_contact)
        }
        if (contacts.isEmpty()) {
            return appContext.getString(R.string.missing_family_email)
        }

        val safeUserName = state.userName.trim().ifBlank { dataStore.getUserName() }
        val resolvedWebhookUrl = state.gasWebhookUrl.trim().ifBlank { dataStore.getGasWebhookUrl() }
        val intervalValue = state.familyInterval.toFloatOrNull()
            ?.takeIf { it >= MIN_DECIMAL_INTERVAL_HOURS && it <= MAX_DECIMAL_INTERVAL_HOURS }
            ?: dataStore.getFamilyNotifyIntervalFloat()

        val results = contacts.map { contact ->
            val subject = EmailContentBuilder.buildSubject(
                recipientTitle = contact.recipientTitle,
                userName = safeUserName,
                isTest = true
            )
            val body = EmailContentBuilder.buildBody(
                recipientTitle = contact.recipientTitle,
                userName = safeUserName,
                intervalHours = intervalValue,
                isTest = true
            )

            WebhookHelper.sendEmail(
                webhookUrl = resolvedWebhookUrl,
                to = contact.email,
                subject = subject,
                body = body
            )
        }
        val successCount = results.count { it.success }
        return if (successCount == contacts.size) {
            if (contacts.size == 1) {
                appContext.getString(R.string.test_email_sent)
            } else {
                appContext.getString(R.string.test_email_sent_multiple, successCount)
            }
        } else {
            appContext.getString(R.string.test_email_partial_failed, successCount, contacts.size)
        }
    }

    fun saveSettings(): Boolean {
        val state = uiState
        val familyContacts = validateFamilyContactsOrNull(state.familyContacts)
        if (familyContacts == null) {
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
        dataStore.setFamilyContacts(familyContacts)
        dataStore.setGasWebhookUrl(state.gasWebhookUrl)
        dataStore.setCareNotificationOn(state.careNotificationEnabled)
        dataStore.setQuietHoursEnabled(state.quietHoursEnabled)
        dataStore.setQuietHoursStartMinutes(quietStartMinutes)
        dataStore.setQuietHoursEndMinutes(quietEndMinutes)

        // 儲存生日（選填，空白時清除）
        val monthStr = state.birthdayMonth.trim()
        val dayStr = state.birthdayDay.trim()
        if (monthStr.isBlank() && dayStr.isBlank()) {
            dataStore.clearBirthday()
        } else {
            val birthdayValidation = validateBirthday(monthStr, dayStr)
            if (birthdayValidation == null) {
                uiState = uiState.copy(birthdayError = true)
                return false
            }
            dataStore.setBirthday(birthdayValidation.first, birthdayValidation.second)
        }

        return true
    }

    private fun updateFamilyContact(index: Int, transform: (FamilyContactUiState) -> FamilyContactUiState) {
        val contacts = uiState.familyContacts.toMutableList()
        if (index !in contacts.indices) return
        contacts[index] = transform(contacts[index])
        uiState = uiState.copy(familyContacts = contacts, emailError = false)
    }

    private fun validateFamilyContactsOrNull(contacts: List<FamilyContactUiState>): List<FamilyContact>? {
        val normalized = mutableListOf<FamilyContact>()
        contacts.forEach { contact ->
            val title = contact.recipientTitle.trim()
            val email = contact.email.trim()
            if (title.isBlank() && email.isBlank()) return@forEach
            if (title.isBlank() || email.isBlank() || !TimeFormatter.isValidEmail(email)) return null
            normalized.add(
                FamilyContact(
                    recipientTitle = title,
                    email = email
                )
            )
        }
        return normalized
    }

    private fun normalizedFamilyContacts(contacts: List<FamilyContactUiState>): List<FamilyContact> {
        return validateFamilyContactsOrNull(contacts).orEmpty()
    }

    /**
     * 驗證生日輸入，回傳 Pair(month, day) 或 null（驗證失敗）。
     * 月份 1–12，日期 1–31，且需符合各月實際天數（含 2/29）。
     */
    private fun validateBirthday(monthStr: String, dayStr: String): Pair<Int, Int>? {
        val month = monthStr.toIntOrNull() ?: return null
        val day = dayStr.toIntOrNull() ?: return null
        if (month !in 1..12) return null
        if (day !in 1..31) return null
        // 各月最大天數（2 月允許 29，以容許 2/29 生日）
        val maxDay = when (month) {
            2 -> 29
            4, 6, 9, 11 -> 30
            else -> 31
        }
        if (day > maxDay) return null
        return Pair(month, day)
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
