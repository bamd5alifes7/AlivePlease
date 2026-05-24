package com.orenhui.aliveplease.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.workers.CareNotificationWorker
import com.orenhui.aliveplease.workers.CheckInReminderWorker
import com.orenhui.aliveplease.workers.FamilyNotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object WorkSchedulerHelper {

    private const val CARE_WORK_NAME = "care_notification"
    private const val FAMILY_WORK_NAME = "family_notification_check"
    private const val DEFERRED_CHECK_IN_WORK_NAME = "check_in_reminder_deferred"
    private const val MIN_CARE_DELAY_MILLIS = 4L * 60L * 60L * 1000L
    private const val MAX_CARE_DELAY_MILLIS = 8L * 60L * 60L * 1000L

    fun rescheduleAll(context: Context) {
        val dataStore = AppDataStore(context)

        if (dataStore.isCareNotificationOn()) {
            scheduleNextCareNotification(context)
        } else {
            cancelCareNotification(context)
        }

        if (dataStore.shouldScheduleFamilyNotification()) {
            scheduleFamilyNotification(context, dataStore.getTimeUntilFamilyNotification())
        } else {
            cancelFamilyNotification(context)
        }
    }

    fun scheduleFamilyNotification(context: Context, delayMillis: Long) {
        val dataStore = AppDataStore(context)
        val safeDelay = if (delayMillis > 0) delayMillis else 1000L
        val request = OneTimeWorkRequestBuilder<FamilyNotificationWorker>()
            .setInitialDelay(safeDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            FAMILY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )

        dataStore.addExecutionLog("已安排家人通知檢查，約 ${safeDelay / 1000} 秒後由系統背景執行。")
    }

    fun cancelFamilyNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FAMILY_WORK_NAME)
        AppDataStore(context).addExecutionLog("已取消家人通知檢查。")
    }

    fun scheduleNextCareNotification(context: Context) {
        val dataStore = AppDataStore(context)
        if (!dataStore.isCareNotificationOn()) {
            cancelCareNotification(context)
            return
        }

        val randomDelay = Random.nextLong(MIN_CARE_DELAY_MILLIS, MAX_CARE_DELAY_MILLIS + 1)
        scheduleCareNotificationAt(context, System.currentTimeMillis() + randomDelay)
    }

    fun scheduleCareNotificationAt(context: Context, triggerAtMillis: Long) {
        val dataStore = AppDataStore(context)
        if (!dataStore.isCareNotificationOn()) {
            cancelCareNotification(context)
            return
        }

        val adjustedTriggerTime = adjustForQuietHours(dataStore, triggerAtMillis)
        val delayMillis = (adjustedTriggerTime - System.currentTimeMillis()).coerceAtLeast(1000L)
        val request = OneTimeWorkRequestBuilder<CareNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            CARE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )

        dataStore.addExecutionLog("已安排關懷提醒，約 ${delayMillis / 60000} 分鐘後由系統背景執行。")
    }

    fun cancelCareNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CARE_WORK_NAME)
        AppDataStore(context).addExecutionLog("已取消關懷提醒。")
    }

    fun scheduleDeferredCheckInReminder(context: Context, triggerAtMillis: Long) {
        val adjustedTriggerTime = adjustForQuietHours(AppDataStore(context), triggerAtMillis)
        val delayMillis = (adjustedTriggerTime - System.currentTimeMillis()).coerceAtLeast(1000L)
        val request = OneTimeWorkRequestBuilder<CheckInReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DEFERRED_CHECK_IN_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun isInQuietHours(dataStore: AppDataStore, timeMillis: Long = System.currentTimeMillis()): Boolean {
        if (!dataStore.isQuietHoursEnabled()) return false

        val currentMinutes = minutesOfDay(timeMillis)
        val startMinutes = dataStore.getQuietHoursStartMinutes()
        val endMinutes = dataStore.getQuietHoursEndMinutes()

        return when {
            startMinutes == endMinutes -> true
            startMinutes < endMinutes -> currentMinutes >= startMinutes && currentMinutes < endMinutes
            else -> currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun adjustForQuietHours(dataStore: AppDataStore, triggerAtMillis: Long): Long {
        if (!isInQuietHours(dataStore, triggerAtMillis)) return triggerAtMillis
        return nextQuietHoursEndMillis(dataStore, triggerAtMillis)
    }

    fun nextQuietHoursEndMillis(
        dataStore: AppDataStore,
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        val endMinutes = dataStore.getQuietHoursEndMinutes()
        val calendar = Calendar.getInstance().apply { timeInMillis = fromMillis }

        calendar.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        calendar.set(Calendar.MINUTE, endMinutes % 60)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= fromMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun minutesOfDay(timeMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
}
