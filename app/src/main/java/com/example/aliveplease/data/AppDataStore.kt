package com.example.aliveplease.data

import android.content.Context
import android.content.SharedPreferences
import com.example.aliveplease.utils.WorkSchedulerHelper
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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
        private const val KEY_FAMILY_EMAIL = "family_email"
        private const val KEY_FAMILY_RECIPIENT_TITLE = "family_recipient_title"
        private const val KEY_GAS_WEBHOOK_URL = "gas_webhook_url"
        private const val KEY_CARE_NOTIFICATION_ON = "care_notification_on"
        private const val KEY_EXECUTION_LOGS = "execution_logs"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_USER_NAME = "user_name"

        private const val DEFAULT_NOTIFY_INTERVAL = 24L
        private const val DEFAULT_FAMILY_NOTIFY_INTERVAL = 48L
        private const val DEFAULT_GAS_WEBHOOK_URL =
            "https://script.google.com/macros/s/AKfycbyYVX3a8BrAozR3UMfjrgiYmHaKePvgCw6BkULBIPav5bYbF4Ij8abvl-BXp2eOJBTC/exec"
        private const val DEFAULT_USER_NAME = "我"
        private const val DEFAULT_RECIPIENT_TITLE = "親愛的家人"

        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
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
        return isNewDay
    }

    fun getAliveDays(): Int = prefs.getInt(KEY_ALIVE_DAYS, 0)

    fun getCurrentStreak(): Int {
        val parsedDates = getCheckInDates()
            .mapNotNull { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
            .sortedDescending()

        if (parsedDates.isEmpty()) return 0

        var streak = 1
        var expectedPreviousDate = parsedDates.first().minusDays(1)

        for (index in 1 until parsedDates.size) {
            val current = parsedDates[index]
            if (current == expectedPreviousDate) {
                streak += 1
                expectedPreviousDate = expectedPreviousDate.minusDays(1)
            } else if (current.isBefore(expectedPreviousDate)) {
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
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

    fun getFamilyNotifyInterval(): Long =
        prefs.getLong(KEY_FAMILY_NOTIFY_INTERVAL, DEFAULT_FAMILY_NOTIFY_INTERVAL)

    fun setFamilyNotifyInterval(hours: Long) {
        prefs.edit().putLong(KEY_FAMILY_NOTIFY_INTERVAL, hours).apply()
    }

    fun getFamilyEmail(): String = prefs.getString(KEY_FAMILY_EMAIL, "").orEmpty()

    fun setFamilyEmail(email: String) {
        prefs.edit().putString(KEY_FAMILY_EMAIL, email.trim()).apply()
    }

    fun getFamilyRecipientTitle(): String {
        val title = prefs.getString(KEY_FAMILY_RECIPIENT_TITLE, DEFAULT_RECIPIENT_TITLE).orEmpty().trim()
        return if (title.isBlank()) DEFAULT_RECIPIENT_TITLE else title
    }

    fun setFamilyRecipientTitle(title: String) {
        prefs.edit().putString(KEY_FAMILY_RECIPIENT_TITLE, title.trim()).apply()
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

    fun getUserName(): String {
        val name = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME).orEmpty().trim()
        return if (name.isBlank()) DEFAULT_USER_NAME else name
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply()
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

    fun shouldNotifyFamily(): Boolean {
        val lastCheckIn = getLastCheckInTime()
        if (lastCheckIn == 0L) return false

        val familyIntervalMillis = (getFamilyNotifyIntervalFloat() * 60 * 60 * 1000).toLong()
        val elapsed = System.currentTimeMillis() - lastCheckIn
        return elapsed >= familyIntervalMillis
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
