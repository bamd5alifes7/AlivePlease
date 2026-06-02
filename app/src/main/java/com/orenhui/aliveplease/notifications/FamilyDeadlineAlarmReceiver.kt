package com.orenhui.aliveplease.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.utils.WorkSchedulerHelper

class FamilyDeadlineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FAMILY_DEADLINE_ALARM) return

        val dataStore = AppDataStore(context)
        if (!dataStore.shouldScheduleFamilyNotification()) {
            dataStore.addExecutionLog("本機親友通知鬧鐘觸發，但目前不需要通知親友。")
            return
        }

        if (!dataStore.shouldNotifyFamily()) {
            WorkSchedulerHelper.scheduleFamilyNotification(
                context,
                dataStore.getTimeUntilFamilyNotification()
            )
            dataStore.addExecutionLog("本機親友通知鬧鐘提早觸發，已依剩餘時間重新排程。")
            return
        }

        NotificationHelper.createNotificationChannels(context)
        NotificationHelper.sendFamilyDeadlineAlert(context)
        dataStore.addExecutionLog("本機親友通知鬧鐘已觸發，已顯示手機通知並啟動寄信流程。")
        WorkSchedulerHelper.enqueueFamilyNotificationNow(context)
    }

    companion object {
        const val ACTION_FAMILY_DEADLINE_ALARM =
            "com.orenhui.aliveplease.ACTION_FAMILY_DEADLINE_ALARM"
    }
}
