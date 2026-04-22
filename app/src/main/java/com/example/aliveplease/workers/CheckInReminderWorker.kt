package com.example.aliveplease.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.aliveplease.notifications.NotificationHelper

/**
 * 打卡提醒 Worker
 * 定期發送打卡提醒通知
 */
class CheckInReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        // 發送打卡提醒通知
        NotificationHelper.sendCheckInReminder(applicationContext)
        
        return Result.success()
    }
}
