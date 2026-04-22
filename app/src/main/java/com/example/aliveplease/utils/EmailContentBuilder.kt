package com.example.aliveplease.utils

import kotlin.math.roundToInt

object EmailContentBuilder {

    fun buildSubject(recipientTitle: String, userName: String, isTest: Boolean = false): String {
        val suffix = if (isTest) "（測試信）" else ""
        return "$recipientTitle，想請你留意一下 $userName$suffix"
    }

    fun buildBody(
        recipientTitle: String,
        userName: String,
        intervalHours: Float,
        isTest: Boolean = false
    ): String {
        val intro = if (isTest) {
            "這是一封測試通知，用來確認設定是否正常。"
        } else {
            "我們已經有一段時間沒有收到 $userName 的報平安。"
        }

        val action = if (isTest) {
            "如果你有收到這封信，代表通知流程已經可以正常送達。"
        } else {
            "如果你方便的話，請主動聯絡一下 $userName ，看看對方是否一切平安。"
        }

        return """
$recipientTitle，你好：

$intro
目前他設定的關心時間是 ${formatIntervalText(intervalHours)}。
$action

這封信由 Alive Please 自動送出，希望在需要的時候，替彼此多留一份心。
        """.trimIndent()
    }

    fun formatIntervalText(intervalHours: Float): String {
        val totalMinutes = (intervalHours * 60).roundToInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "$hours 小時 $minutes 分鐘"
            hours > 0 -> "$hours 小時"
            else -> "$minutes 分鐘"
        }
    }
}
