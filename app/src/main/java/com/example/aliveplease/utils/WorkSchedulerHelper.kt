package com.example.aliveplease.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.notifications.CareAlarmReceiver
import com.example.aliveplease.notifications.FamilyAlarmReceiver
import com.example.aliveplease.workers.CheckInReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object WorkSchedulerHelper {

    private const val ALARM_REQUEST_CODE = 9999
    private const val CARE_ALARM_REQUEST_CODE = 10001
    private const val MIN_CARE_DELAY_MILLIS = 4L * 60L * 60L * 1000L
    private const val MAX_CARE_DELAY_MILLIS = 8L * 60L * 60L * 1000L
    private const val DEFERRED_CHECK_IN_WORK_NAME = "check_in_reminder_deferred"

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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, FamilyAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val safeDelay = if (delayMillis > 0) delayMillis else 1000L
        val triggerAtMillis = System.currentTimeMillis() + safeDelay

        dataStore.addExecutionLog("已安排親友通知，約 ${safeDelay / 1000} 秒後觸發。")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            dataStore.addExecutionLog("親友通知精準排程失敗，改用一般排程。")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelFamilyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FamilyAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        AppDataStore(context).addExecutionLog("已取消親友通知排程。")
    }

    fun scheduleNextCareNotification(context: Context) {
        val dataStore = AppDataStore(context)
        if (!dataStore.isCareNotificationOn()) {
            cancelCareNotification(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createCarePendingIntent(context)
        alarmManager.cancel(pendingIntent)

        val randomDelay = Random.nextLong(MIN_CARE_DELAY_MILLIS, MAX_CARE_DELAY_MILLIS + 1)
        val rawTriggerTime = System.currentTimeMillis() + randomDelay
        val adjustedTriggerTime = adjustForQuietHours(dataStore, rawTriggerTime)
        val delayMinutes = (adjustedTriggerTime - System.currentTimeMillis()) / 60000

        dataStore.addExecutionLog("已安排下次關懷提醒，約 $delayMinutes 分鐘後觸發。")
        scheduleCareAlarm(alarmManager, pendingIntent, adjustedTriggerTime, dataStore)
    }

    fun scheduleCareNotificationAt(context: Context, triggerAtMillis: Long) {
        val dataStore = AppDataStore(context)
        if (!dataStore.isCareNotificationOn()) {
            cancelCareNotification(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createCarePendingIntent(context)
        alarmManager.cancel(pendingIntent)

        val adjustedTriggerTime = adjustForQuietHours(dataStore, triggerAtMillis)
        val delayMinutes = (adjustedTriggerTime - System.currentTimeMillis()).coerceAtLeast(0L) / 60000
        dataStore.addExecutionLog("關懷提醒已延後，約 $delayMinutes 分鐘後觸發。")
        scheduleCareAlarm(alarmManager, pendingIntent, adjustedTriggerTime, dataStore)
    }

    fun cancelCareNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createCarePendingIntent(context)
        alarmManager.cancel(pendingIntent)
        AppDataStore(context).addExecutionLog("已取消關懷提醒排程。")
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

    private fun createCarePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CareAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            CARE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun minutesOfDay(timeMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun scheduleCareAlarm(
        alarmManager: AlarmManager,
        pendingIntent: PendingIntent,
        triggerAtMillis: Long,
        dataStore: AppDataStore
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            dataStore.addExecutionLog("關懷提醒精準排程失敗，改用一般排程。")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
