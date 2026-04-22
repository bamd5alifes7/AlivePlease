package com.example.aliveplease.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.utils.EmailContentBuilder
import com.example.aliveplease.utils.WebhookHelper

class FamilyNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dataStore = AppDataStore(applicationContext)

        if (!dataStore.shouldNotifyFamily()) {
            return Result.success()
        }

        val familyEmail = dataStore.getFamilyEmail()
        val webhookUrl = dataStore.getGasWebhookUrl()
        if (familyEmail.isBlank() || webhookUrl.isBlank()) {
            return Result.success()
        }

        val subject = EmailContentBuilder.buildSubject(
            recipientTitle = dataStore.getFamilyRecipientTitle(),
            userName = dataStore.getUserName()
        )
        val body = EmailContentBuilder.buildBody(
            recipientTitle = dataStore.getFamilyRecipientTitle(),
            userName = dataStore.getUserName(),
            intervalHours = dataStore.getFamilyNotifyIntervalFloat()
        )

        val result = WebhookHelper.sendEmail(
            webhookUrl = webhookUrl,
            to = familyEmail,
            subject = subject,
            body = body
        )

        result.message?.let { dataStore.addExecutionLog(it) }

        return if (result.success) Result.success() else Result.retry()
    }
}
