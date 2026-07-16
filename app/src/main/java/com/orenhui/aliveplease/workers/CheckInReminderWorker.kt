package com.orenhui.aliveplease.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.notifications.NotificationHelper
import com.orenhui.aliveplease.utils.WorkSchedulerHelper

class CheckInReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val dataStore = AppDataStore(applicationContext)
        if (dataStore.isFirstLaunch()) {
            WorkSchedulerHelper.cancelCheckInReminder(applicationContext)
            return Result.success()
        }

        if (!dataStore.shouldSendCheckInReminder()) {
            WorkSchedulerHelper.scheduleCheckInReminder(applicationContext)
            return Result.success()
        }

        if (WorkSchedulerHelper.isInQuietHours(dataStore)) {
            val quietEndMillis = WorkSchedulerHelper.nextQuietHoursEndMillis(dataStore)
            WorkSchedulerHelper.scheduleDeferredCheckInReminder(applicationContext, quietEndMillis)
            dataStore.addExecutionLog("打卡提醒遇到安靜時段，已延後到安靜時段結束。")
            return Result.success()
        }

        NotificationHelper.sendCheckInReminder(applicationContext)
        return Result.success()
    }
}
