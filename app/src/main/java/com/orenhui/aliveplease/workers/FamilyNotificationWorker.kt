package com.orenhui.aliveplease.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.notifications.NotificationHelper
import com.orenhui.aliveplease.utils.EmailContentBuilder
import com.orenhui.aliveplease.utils.WebhookHelper

class FamilyNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dataStore = AppDataStore(applicationContext)

        if (!dataStore.shouldScheduleFamilyNotification() || !dataStore.shouldNotifyFamily()) {
            return Result.success()
        }

        val familyEmail = dataStore.getFamilyEmail()
        val webhookUrl = dataStore.getGasWebhookUrl()
        if (familyEmail.isBlank() || webhookUrl.isBlank()) {
            dataStore.addExecutionLog("未寄出通知：通知服務或收件人 Email 尚未設定。")
            return Result.success()
        }

        val recipientTitle = dataStore.getFamilyRecipientTitle()
        val userName = dataStore.getUserName()
        val intervalHours = dataStore.getFamilyNotifyIntervalFloat()
        val subject = EmailContentBuilder.buildSubject(recipientTitle, userName)
        val body = EmailContentBuilder.buildBody(recipientTitle, userName, intervalHours)

        dataStore.addExecutionLog("正在透過通知服務寄出家人通知：$familyEmail")

        val result = WebhookHelper.sendEmail(
            webhookUrl = webhookUrl,
            to = familyEmail,
            subject = subject,
            body = body
        )

        return if (result.success) {
            dataStore.addExecutionLog("家人通知已寄出：$familyEmail")
            NotificationHelper.sendFamilyNotificationStatus(applicationContext, true, familyEmail)
            Result.success()
        } else {
            val message = result.message ?: "通知服務沒有回傳成功結果。"
            dataStore.addExecutionLog("家人通知寄送失敗：$message")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                return Result.retry()
            }
            NotificationHelper.sendFamilyNotificationStatus(applicationContext, false, familyEmail)
            Result.success()
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 2
    }
}
