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

        val notificationCycleId = dataStore.getFamilyNotificationCycleId()
        val contacts = dataStore.getPendingFamilyNotificationContacts()
        val webhookUrl = dataStore.getGasWebhookUrl()
        if (contacts.isEmpty() || webhookUrl.isBlank()) {
            dataStore.addExecutionLog("親友通知取消：收件人 Email 或寄信服務尚未設定。")
            return Result.success()
        }

        val userName = dataStore.getUserName()
        val intervalHours = dataStore.getFamilyNotifyIntervalFloat()
        val failedMessages = mutableListOf<String>()

        contacts.forEach { contact ->
            if (!dataStore.isCurrentFamilyNotificationCycle(notificationCycleId)) {
                return Result.success()
            }

            val subject = EmailContentBuilder.buildSubject(contact.recipientTitle, userName)
            val body = EmailContentBuilder.buildBody(contact.recipientTitle, userName, intervalHours)

            dataStore.addExecutionLog("正在透過通知服務寄出親友通知：${contact.email}")

            val result = WebhookHelper.sendEmail(
                webhookUrl = webhookUrl,
                to = contact.email,
                subject = subject,
                body = body,
                requestId = dataStore.getFamilyNotificationRequestId(notificationCycleId, contact.email)
            )

            if (result.success) {
                dataStore.markFamilyNotificationSent(notificationCycleId, contact.email)
                dataStore.addExecutionLog("親友通知已寄出：${contact.email}")
            } else {
                failedMessages.add(result.message ?: "未知錯誤")
                dataStore.addExecutionLog("親友通知寄送失敗：${contact.email}")
            }
        }

        if (!dataStore.isCurrentFamilyNotificationCycle(notificationCycleId)) {
            return Result.success()
        }

        val remainingContacts = dataStore.getPendingFamilyNotificationContacts()
        val allContacts = dataStore.getFamilyContacts()

        return if (remainingContacts.isEmpty()) {
            NotificationHelper.sendFamilyNotificationStatus(
                applicationContext,
                true,
                formatRecipientSummary(allContacts.map { it.email })
            )
            Result.success()
        } else {
            val message = failedMessages.firstOrNull() ?: "未知錯誤"
            dataStore.addExecutionLog("親友通知部分或全部寄送失敗：$message")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                return Result.retry()
            }
            NotificationHelper.sendFamilyNotificationStatus(
                applicationContext,
                false,
                "${allContacts.size - remainingContacts.size}/${allContacts.size} 位親友"
            )
            Result.success()
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 2

        private fun formatRecipientSummary(emails: List<String>): String {
            return if (emails.size == 1) emails.first() else "${emails.size} 位親友"
        }
    }
}
