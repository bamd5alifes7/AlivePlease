package com.example.aliveplease.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.utils.WorkSchedulerHelper

class CareAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val dataStore = AppDataStore(context)

        if (!dataStore.isCareNotificationOn()) {
            WorkSchedulerHelper.cancelCareNotification(context)
            return
        }

        val message = NotificationHelper.getRandomCareMessage(context)
        NotificationHelper.sendCareMessage(context, message)
        dataStore.addExecutionLog("已送出一則關懷提醒，接著安排下一次隨機提醒。")

        WorkSchedulerHelper.scheduleNextCareNotification(context)
    }
}
