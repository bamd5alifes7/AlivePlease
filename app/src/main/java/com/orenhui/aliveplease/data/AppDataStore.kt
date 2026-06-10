package com.orenhui.aliveplease.data

import android.content.Context
import android.content.SharedPreferences
import com.orenhui.aliveplease.utils.WorkSchedulerHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class FamilyContact(
    val recipientTitle: String,
    val email: String
)

class AppDataStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "alive_please_prefs"

        private const val KEY_LAST_CHECK_IN_TIME = "last_check_in_time"
        private const val KEY_ALIVE_DAYS = "alive_days"
        private const val KEY_CHECK_IN_DATES = "check_in_dates"
        private const val KEY_NOTIFY_INTERVAL = "notify_interval"
        private const val KEY_FAMILY_NOTIFY_INTERVAL = "family_notify_interval"
        private const val KEY_FAMILY_NOTIFY_INTERVAL_FLOAT = "family_notify_interval_float"
        private const val KEY_FAMILY_WARNING_BEFORE_HOURS = "family_warning_before_hours"
        private const val KEY_FAMILY_EMAIL = "family_email"
        private const val KEY_FAMILY_CONTACTS_JSON = "family_contacts_json"
        private const val KEY_FAMILY_RECIPIENT_TITLE = "family_recipient_title"
        private const val KEY_GAS_WEBHOOK_URL = "gas_webhook_url"
        private const val KEY_CARE_NOTIFICATION_ON = "care_notification_on"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START_MINUTES = "quiet_hours_start_minutes"
        private const val KEY_QUIET_HOURS_END_MINUTES = "quiet_hours_end_minutes"
        private const val KEY_EXECUTION_LOGS = "execution_logs"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_SETUP_TUTORIAL_PENDING = "setup_tutorial_pending"
        private const val KEY_USER_NAME = "user_name"

        private const val DEFAULT_NOTIFY_INTERVAL = 12L
        private const val DEFAULT_FAMILY_NOTIFY_INTERVAL = 28L
        private const val DEFAULT_FAMILY_WARNING_BEFORE_HOURS = 0.5f
        private const val DEFAULT_QUIET_HOURS_START_MINUTES = 23 * 60
        private const val DEFAULT_QUIET_HOURS_END_MINUTES = 7 * 60
        private const val DEFAULT_GAS_WEBHOOK_URL =
            "https://script.google.com/macros/s/AKfycbyYVX3a8BrAozR3UMfjrgiYmHaKePvgCw6BkULBIPav5bYbF4Ij8abvl-BXp2eOJBTC/exec"
        private const val DEFAULT_USER_NAME = "我"
        private const val DEFAULT_RECIPIENT_TITLE = "親愛的家人"

        private const val DATE_PATTERN = "yyyy-MM-dd"
    }

    fun getLastCheckInTime(): Long = prefs.getLong(KEY_LAST_CHECK_IN_TIME, 0L)

    fun setLastCheckInTime(timeMillis: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_IN_TIME, timeMillis).apply()
    }

    fun performCheckIn(): Boolean {
        val now = System.currentTimeMillis()
        val today = getTodayDateString()

        setLastCheckInTime(now)

        val checkInDates = getCheckInDates().toMutableSet()
        val isNewDay = today !in checkInDates

        if (isNewDay) {
            checkInDates.add(today)
            saveCheckInDates(checkInDates)
            setAliveDays(checkInDates.size)
        }

        WorkSchedulerHelper.scheduleFamilyNotification(context, getTimeUntilFamilyNotification())
        WorkSchedulerHelper.scheduleCheckInReminder(context)
        return isNewDay
    }

    fun getAliveDays(): Int = prefs.getInt(KEY_ALIVE_DAYS, 0)

    fun getCurrentStreak(): Int {
        val parsedDates = getCheckInDates()
            .mapNotNull(::parseCheckInDateMillis)
            .sortedDescending()

        if (parsedDates.isEmpty()) return 0

        var streak = 1
        var expectedPreviousDate = minusOneDay(parsedDates.first())

        for (index in 1 until parsedDates.size) {
            val current = parsedDates[index]
            if (current == expectedPreviousDate) {
                streak += 1
                expectedPreviousDate = minusOneDay(expectedPreviousDate)
            } else if (current < expectedPreviousDate) {
                break
            }
        }

        return streak
    }

    private fun setAliveDays(days: Int) {
        prefs.edit().putInt(KEY_ALIVE_DAYS, days).apply()
    }

    fun getCheckInDates(): Set<String> {
        val datesString = prefs.getString(KEY_CHECK_IN_DATES, "").orEmpty()
        return if (datesString.isBlank()) emptySet() else datesString.split(",").toSet()
    }

    private fun saveCheckInDates(dates: Set<String>) {
        prefs.edit().putString(KEY_CHECK_IN_DATES, dates.joinToString(",")).apply()
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return dateFormat.format(Date())
    }

    private fun parseCheckInDateMillis(value: String): Long? {
        val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
        }
        return runCatching { dateFormat.parse(value)?.time }.getOrNull()
    }

    private fun minusOneDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
    }

    fun getNotifyInterval(): Long = prefs.getLong(KEY_NOTIFY_INTERVAL, DEFAULT_NOTIFY_INTERVAL)

    fun setNotifyInterval(hours: Long) {
        prefs.edit().putLong(KEY_NOTIFY_INTERVAL, hours).apply()
    }

    fun getFamilyNotifyIntervalFloat(): Float {
        val fallback = getFamilyNotifyInterval().toFloat()
        return prefs.getFloat(KEY_FAMILY_NOTIFY_INTERVAL_FLOAT, fallback)
    }

    fun setFamilyNotifyIntervalFloat(hours: Float) {
        prefs.edit().putFloat(KEY_FAMILY_NOTIFY_INTERVAL_FLOAT, hours).apply()
        WorkSchedulerHelper.scheduleFamilyNotification(context, getTimeUntilFamilyNotification())
    }

    fun getFamilyWarningBeforeHours(): Float =
        prefs.getFloat(KEY_FAMILY_WARNING_BEFORE_HOURS, DEFAULT_FAMILY_WARNING_BEFORE_HOURS)

    fun setFamilyWarningBeforeHours(hours: Float) {
        prefs.edit().putFloat(KEY_FAMILY_WARNING_BEFORE_HOURS, hours).apply()
        WorkSchedulerHelper.scheduleFamilyNotification(context, getTimeUntilFamilyNotification())
    }

    fun getFamilyNotifyInterval(): Long =
        prefs.getLong(KEY_FAMILY_NOTIFY_INTERVAL, DEFAULT_FAMILY_NOTIFY_INTERVAL)

    fun setFamilyNotifyInterval(hours: Long) {
        prefs.edit().putLong(KEY_FAMILY_NOTIFY_INTERVAL, hours).apply()
    }

    fun getFamilyEmail(): String = prefs.getString(KEY_FAMILY_EMAIL, "").orEmpty()

    fun setFamilyEmail(email: String) {
        prefs.edit().putString(KEY_FAMILY_EMAIL, email.trim()).apply()
    }

    fun getFamilyContacts(): List<FamilyContact> {
        val storedContacts = parseFamilyContacts(
            prefs.getString(KEY_FAMILY_CONTACTS_JSON, "").orEmpty()
        )
        if (storedContacts.isNotEmpty()) return storedContacts

        val legacyEmail = getFamilyEmail().trim()
        if (legacyEmail.isBlank()) return emptyList()

        return listOf(
            FamilyContact(
                recipientTitle = getFamilyRecipientTitle(),
                email = legacyEmail
            )
        )
    }

    fun setFamilyContacts(contacts: List<FamilyContact>) {
        val normalizedContacts = contacts.mapNotNull { contact ->
            val email = contact.email.trim()
            if (email.isBlank()) {
                null
            } else {
                FamilyContact(
                    recipientTitle = contact.recipientTitle.trim().ifBlank { DEFAULT_RECIPIENT_TITLE },
                    email = email
                )
            }
        }
        val json = JSONArray().apply {
            normalizedContacts.forEach { contact ->
                put(
                    JSONObject()
                        .put("recipientTitle", contact.recipientTitle)
                        .put("email", contact.email)
                )
            }
        }.toString()
        val firstContact = normalizedContacts.firstOrNull()

        prefs.edit()
            .putString(KEY_FAMILY_CONTACTS_JSON, json)
            .putString(KEY_FAMILY_EMAIL, firstContact?.email.orEmpty())
            .putString(KEY_FAMILY_RECIPIENT_TITLE, firstContact?.recipientTitle.orEmpty())
            .apply()
    }

    fun hasCheckedIn(): Boolean = getLastCheckInTime() > 0L

    fun hasFamilyNotificationRecipient(): Boolean = getFamilyContacts().isNotEmpty()

    fun shouldScheduleFamilyNotification(): Boolean =
        hasCheckedIn() && hasFamilyNotificationRecipient()

    fun getFamilyRecipientTitle(): String {
        val title = prefs.getString(KEY_FAMILY_RECIPIENT_TITLE, DEFAULT_RECIPIENT_TITLE).orEmpty().trim()
        return if (title.isBlank()) DEFAULT_RECIPIENT_TITLE else title
    }

    fun setFamilyRecipientTitle(title: String) {
        prefs.edit().putString(KEY_FAMILY_RECIPIENT_TITLE, title.trim()).apply()
    }

    private fun parseFamilyContacts(rawJson: String): List<FamilyContact> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val email = item.optString("email").trim()
                    if (email.isBlank()) continue
                    val recipientTitle = item.optString("recipientTitle").trim()
                        .ifBlank { DEFAULT_RECIPIENT_TITLE }
                    add(FamilyContact(recipientTitle, email))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun getStoredGasWebhookUrl(): String =
        prefs.getString(KEY_GAS_WEBHOOK_URL, "").orEmpty().trim()

    fun getGasWebhookUrl(): String {
        val storedUrl = getStoredGasWebhookUrl()
        return if (storedUrl.isBlank()) DEFAULT_GAS_WEBHOOK_URL else storedUrl
    }

    fun setGasWebhookUrl(url: String) {
        prefs.edit().putString(KEY_GAS_WEBHOOK_URL, url.trim()).apply()
    }

    fun isCareNotificationOn(): Boolean = prefs.getBoolean(KEY_CARE_NOTIFICATION_ON, true)

    fun setCareNotificationOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CARE_NOTIFICATION_ON, enabled).apply()
    }

    fun isQuietHoursEnabled(): Boolean = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, true)

    fun setQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun getQuietHoursStartMinutes(): Int =
        prefs.getInt(KEY_QUIET_HOURS_START_MINUTES, DEFAULT_QUIET_HOURS_START_MINUTES)

    fun setQuietHoursStartMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_QUIET_HOURS_START_MINUTES, minutes.coerceIn(0, 23 * 60 + 59)).apply()
    }

    fun getQuietHoursEndMinutes(): Int =
        prefs.getInt(KEY_QUIET_HOURS_END_MINUTES, DEFAULT_QUIET_HOURS_END_MINUTES)

    fun setQuietHoursEndMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_QUIET_HOURS_END_MINUTES, minutes.coerceIn(0, 23 * 60 + 59)).apply()
    }

    fun getUserName(): String {
        val name = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME).orEmpty().trim()
        return if (name.isBlank()) DEFAULT_USER_NAME else name
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).commit()
    }

    fun setSetupTutorialPending() {
        prefs.edit().putBoolean(KEY_SETUP_TUTORIAL_PENDING, true).commit()
    }

    fun clearSetupTutorialPending() {
        prefs.edit().putBoolean(KEY_SETUP_TUTORIAL_PENDING, false).apply()
    }

    fun consumeSetupTutorialPending(): Boolean {
        if (!prefs.getBoolean(KEY_SETUP_TUTORIAL_PENDING, false)) return false
        clearSetupTutorialPending()
        return true
    }

    fun getTimeUntilCheckInReminder(): Long {
        val lastCheckIn = getLastCheckInTime()
        if (lastCheckIn == 0L) return 0L

        val elapsed = System.currentTimeMillis() - lastCheckIn
        val remaining = TimeUnit.HOURS.toMillis(getNotifyInterval()) - elapsed
        return if (remaining > 0) remaining else 0L
    }

    fun shouldSendCheckInReminder(): Boolean {
        return getTimeUntilCheckInReminder() <= 0L
    }

    fun getTimeUntilFamilyNotification(): Long {
        val lastCheckIn = getLastCheckInTime()
        val familyIntervalMillis = (getFamilyNotifyIntervalFloat() * 60 * 60 * 1000).toLong()

        if (lastCheckIn == 0L) {
            return familyIntervalMillis
        }

        val elapsed = System.currentTimeMillis() - lastCheckIn
        val remaining = familyIntervalMillis - elapsed
        return if (remaining > 0) remaining else 0L
    }

    fun getTimeUntilFamilyWarning(): Long {
        val notificationRemaining = getTimeUntilFamilyNotification()
        val warningBeforeMillis = (getFamilyWarningBeforeHours() * 60 * 60 * 1000).toLong()
        val warningRemaining = notificationRemaining - warningBeforeMillis
        return if (warningRemaining > 0) warningRemaining else 0L
    }

    fun shouldNotifyFamily(): Boolean {
        val lastCheckIn = getLastCheckInTime()
        if (lastCheckIn == 0L) return false

        val familyIntervalMillis = (getFamilyNotifyIntervalFloat() * 60 * 60 * 1000).toLong()
        val elapsed = System.currentTimeMillis() - lastCheckIn
        return elapsed >= familyIntervalMillis
    }

    fun shouldSendFamilyWarning(): Boolean {
        if (!shouldScheduleFamilyNotification()) return false
        val remaining = getTimeUntilFamilyNotification()
        val warningBeforeMillis = (getFamilyWarningBeforeHours() * 60 * 60 * 1000).toLong()
        return remaining in 1..warningBeforeMillis
    }

    fun addExecutionLog(message: String) {
        val formatter = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
        val timestamp = formatter.format(Date())
        val logEntry = "[$timestamp] $message"

        val logs = getExecutionLogs().toMutableList()
        logs.add(0, logEntry)
        if (logs.size > 50) {
            logs.removeAt(logs.lastIndex)
        }

        prefs.edit().putString(KEY_EXECUTION_LOGS, logs.joinToString("||")).apply()
    }

    fun getExecutionLogs(): List<String> {
        val logString = prefs.getString(KEY_EXECUTION_LOGS, "").orEmpty()
        return if (logString.isBlank()) emptyList() else logString.split("||")
    }
}
