package com.example.aliveplease.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object WebhookHelper {
    private val client = OkHttpClient()

    data class EmailSendResult(
        val success: Boolean,
        val message: String? = null
    )

    suspend fun sendEmail(
        webhookUrl: String,
        to: String,
        subject: String,
        body: String
    ): EmailSendResult {
        if (webhookUrl.isBlank() || to.isBlank()) {
            return EmailSendResult(
                success = false,
                message = "Webhook URL or recipient email is empty."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val jsonPayload = JSONObject()
                    .put("to", to)
                    .put("subject", subject)
                    .put("body", body)
                    .toString()

                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(jsonPayload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@use EmailSendResult(
                            success = false,
                            message = parseErrorMessage(responseBody)
                                ?: "Webhook request failed with HTTP ${response.code}."
                        )
                    }

                    val responseJson = responseBody.toJsonObjectOrNull()
                    val ok = responseJson?.optBoolean("ok", true) ?: true
                    val message = responseJson?.optString("error")
                        ?.takeIf { !it.isNullOrBlank() }
                        ?: responseJson?.optString("message")?.takeIf { !it.isNullOrBlank() }

                    if (!ok) {
                        EmailSendResult(
                            success = false,
                            message = message ?: "GAS returned failure."
                        )
                    } else {
                        EmailSendResult(
                            success = true,
                            message = message
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                EmailSendResult(
                    success = false,
                    message = e.message ?: "Unknown error."
                )
            }
        }
    }

    private fun parseErrorMessage(responseBody: String): String? {
        return responseBody.toJsonObjectOrNull()
            ?.optString("error")
            ?.takeIf { !it.isNullOrBlank() }
    }

    private fun String.toJsonObjectOrNull(): JSONObject? {
        return runCatching { JSONObject(this) }.getOrNull()
    }
}
