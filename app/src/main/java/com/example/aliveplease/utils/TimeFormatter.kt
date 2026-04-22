package com.example.aliveplease.utils

import android.content.Context
import com.example.aliveplease.R

/**
 * 時間格式化工具類
 */
object TimeFormatter {
    
    /**
     * 格式化倒數時間
     * @param milliseconds 毫秒數
     * @return 格式化的時間字串，例如 "23小時15分鐘" 或 "1天5小時"
     */
    fun formatCountdown(context: Context, milliseconds: Long): String {
        if (milliseconds <= 0) {
            return context.getString(R.string.time_format_seconds, 0)
        }
        
        val totalSeconds = milliseconds / 1000
        val totalMinutes = totalSeconds / 60
        val totalHours = totalMinutes / 60
        val days = totalHours / 24
        
        val displaySeconds = totalSeconds % 60
        val displayMinutes = totalMinutes % 60
        
        return when {
            days > 0 -> {
                val remainingHours = totalHours % 24
                if (remainingHours > 0) {
                    context.getString(R.string.time_format_days_hours, days, remainingHours)
                } else {
                    context.getString(R.string.time_format_days, days)
                }
            }
            totalHours > 0 -> {
                if (displaySeconds > 0) {
                    context.getString(R.string.time_format_hours_minutes_seconds, totalHours, displayMinutes, displaySeconds)
                } else if (displayMinutes > 0) {
                    context.getString(R.string.time_format_hours_minutes, totalHours, displayMinutes)
                } else {
                    context.getString(R.string.time_format_hours, totalHours)
                }
            }
            totalMinutes > 0 -> {
                if (displaySeconds > 0) {
                    context.getString(R.string.time_format_minutes_seconds, totalMinutes, displaySeconds)
                } else {
                    context.getString(R.string.time_format_minutes, totalMinutes)
                }
            }
            else -> {
                context.getString(R.string.time_format_seconds, displaySeconds)
            }
        }
    }
    
    /**
     * 驗證 Email 格式
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
