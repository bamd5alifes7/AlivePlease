package com.orenhui.aliveplease.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.notifications.NotificationHelper

class FamilyWarningWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val dataStore = AppDataStore(applicationContext)
        if (!dataStore.shouldSendFamilyWarning()) {
            return Result.success()
        }

        NotificationHelper.sendFamilyWarning(applicationContext, dataStore.getTimeUntilFamilyNotification())
        dataStore.addExecutionLog("已發出親友通知前提醒。")
        return Result.success()
    }
}
