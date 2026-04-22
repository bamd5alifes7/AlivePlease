package com.example.aliveplease.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.notifications.CareAlarmReceiver
import com.example.aliveplease.notifications.FamilyAlarmReceiver
import java.util.Calendar
import kotlin.random.Random

object WorkSchedulerHelper {

    private const val ALARM_REQUEST_CODE = 9999
    private const val CARE_ALARM_REQUEST_CODE = 10001
    private const val MIN_CARE_DELAY_MILLIS = 4L * 60L * 60L * 1000L
    private const val MAX_CARE_DELAY_MILLIS = 8L * 60L * 60L * 1000L
    private const val SLEEP_START_HOUR = 23
    private const val SLEEP_END_HOUR = 7

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

        dataStore.addExecutionLog("已安排家人通知，${safeDelay / 1000} 秒後觸發。")

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
            dataStore.addExecutionLog("家人通知精準排程失敗，改用一般排程。")
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

        AppDataStore(context).addExecutionLog("已取消家人通知排程。")
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
        val adjustedTriggerTime = adjustForSleepHours(rawTriggerTime)
        val delayMinutes = (adjustedTriggerTime - System.currentTimeMillis()) / 60000

        dataStore.addExecutionLog("已安排下次關懷提醒，約 $delayMinutes 分鐘後觸發。")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    adjustedTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    adjustedTriggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            dataStore.addExecutionLog("關懷提醒精準排程失敗，改用一般排程。")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                adjustedTriggerTime,
                pendingIntent
            )
        }
    }

    fun cancelCareNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createCarePendingIntent(context)
        alarmManager.cancel(pendingIntent)
        AppDataStore(context).addExecutionLog("已取消關懷提醒排程。")
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

    private fun adjustForSleepHours(triggerAtMillis: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isSleeping = hour >= SLEEP_START_HOUR || hour < SLEEP_END_HOUR
        if (!isSleeping) return triggerAtMillis

        if (hour >= SLEEP_START_HOUR) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, SLEEP_END_HOUR)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
