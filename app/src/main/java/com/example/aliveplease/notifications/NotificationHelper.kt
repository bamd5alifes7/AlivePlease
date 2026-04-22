package com.example.aliveplease.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aliveplease.MainActivity
import com.example.aliveplease.R

object NotificationHelper {

    private const val CHANNEL_ID_CHECK_IN = "check_in_reminder"
    private const val CHANNEL_ID_CARE = "care_message"
    private const val CHANNEL_ID_FAMILY_STATUS = "family_status"

    private const val NOTIFICATION_ID_CHECK_IN = 1001
    private const val NOTIFICATION_ID_CARE = 1002
    private const val NOTIFICATION_ID_FAMILY_STATUS = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val checkInChannel = NotificationChannel(
                CHANNEL_ID_CHECK_IN,
                context.getString(R.string.notification_channel_checkin_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_checkin_desc)
                enableVibration(true)
            }

            val careChannel = NotificationChannel(
                CHANNEL_ID_CARE,
                context.getString(R.string.notification_channel_care_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_care_desc)
                enableVibration(false)
            }

            val familyStatusChannel = NotificationChannel(
                CHANNEL_ID_FAMILY_STATUS,
                context.getString(R.string.notification_channel_family_status_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_family_status_desc)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(checkInChannel)
            notificationManager.createNotificationChannel(careChannel)
            notificationManager.createNotificationChannel(familyStatusChannel)
        }
    }

    fun sendCheckInReminder(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHECK_IN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_checkin_title))
            .setContentText(context.getString(R.string.notification_checkin_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityPendingIntent(context))
            .build()

        notifySafely(context, NOTIFICATION_ID_CHECK_IN, notification)
    }

    fun sendCareMessage(context: Context, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CARE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_care_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityPendingIntent(context))
            .build()

        notifySafely(context, NOTIFICATION_ID_CARE, notification)
    }

    fun sendFamilyNotificationStatus(context: Context, isSuccess: Boolean, familyEmail: String) {
        val title = if (isSuccess) {
            context.getString(R.string.notification_family_status_success_title)
        } else {
            context.getString(R.string.notification_family_status_failure_title)
        }
        val message = if (isSuccess) {
            context.getString(R.string.notification_family_status_success_message, familyEmail)
        } else {
            context.getString(R.string.notification_family_status_failure_message, familyEmail)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FAMILY_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityPendingIntent(context))
            .build()

        notifySafely(context, NOTIFICATION_ID_FAMILY_STATUS, notification)
    }

    fun getRandomCareMessage(context: Context): String {
        val messages = context.resources.getStringArray(R.array.care_messages)
        return messages.random()
    }

    private fun createMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun notifySafely(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
