package com.example.aliveplease.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.utils.EmailContentBuilder
import com.example.aliveplease.utils.WebhookHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FamilyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val dataStore = AppDataStore(context)

        if (!dataStore.shouldScheduleFamilyNotification() || !dataStore.shouldNotifyFamily()) {
            pendingResult.finish()
            return
        }

        dataStore.addExecutionLog("家人通知排程已觸發，準備檢查是否需要寄信。")

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val familyEmail = dataStore.getFamilyEmail()
            try {
                val webhookUrl = dataStore.getGasWebhookUrl()

                if (webhookUrl.isBlank() || familyEmail.isBlank()) {
                    dataStore.addExecutionLog("未寄出通知：Webhook URL 或收件人 Email 尚未設定。")
                    return@launch
                }

                val recipientTitle = dataStore.getFamilyRecipientTitle()
                val userName = dataStore.getUserName()
                val intervalHours = dataStore.getFamilyNotifyIntervalFloat()

                val subject = EmailContentBuilder.buildSubject(recipientTitle, userName)
                val body = EmailContentBuilder.buildBody(recipientTitle, userName, intervalHours)

                dataStore.addExecutionLog("開始寄送家人通知信到 $familyEmail。")

                val result = WebhookHelper.sendEmail(
                    webhookUrl = webhookUrl,
                    to = familyEmail,
                    subject = subject,
                    body = body
                )

                if (result.success) {
                    dataStore.addExecutionLog("家人通知寄送成功：$familyEmail")
                    NotificationHelper.sendFamilyNotificationStatus(context, true, familyEmail)
                } else {
                    dataStore.addExecutionLog("家人通知寄送失敗，Webhook 沒有回傳成功結果。")
                    NotificationHelper.sendFamilyNotificationStatus(context, false, familyEmail)
                }
            } catch (e: Exception) {
                dataStore.addExecutionLog("寄送家人通知時發生錯誤：${e.message}")
                NotificationHelper.sendFamilyNotificationStatus(
                    context,
                    false,
                    familyEmail.ifBlank { "收件人" }
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
