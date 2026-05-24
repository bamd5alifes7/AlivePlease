package com.orenhui.aliveplease.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.notifications.NotificationHelper
import com.orenhui.aliveplease.utils.WorkSchedulerHelper

class CareNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val dataStore = AppDataStore(applicationContext)

        if (!dataStore.isCareNotificationOn()) {
            WorkSchedulerHelper.cancelCareNotification(applicationContext)
            return Result.success()
        }

        if (WorkSchedulerHelper.isInQuietHours(dataStore)) {
            val quietEndMillis = WorkSchedulerHelper.nextQuietHoursEndMillis(dataStore)
            WorkSchedulerHelper.scheduleCareNotificationAt(applicationContext, quietEndMillis)
            dataStore.addExecutionLog("關懷提醒遇到安靜時段，已延後到安靜時段結束。")
            return Result.success()
        }

        val message = NotificationHelper.getRandomCareMessage(applicationContext)
        NotificationHelper.sendCareMessage(applicationContext, message)
        dataStore.addExecutionLog("已送出一則關懷提醒。")

        WorkSchedulerHelper.scheduleNextCareNotification(applicationContext)
        return Result.success()
    }
}
