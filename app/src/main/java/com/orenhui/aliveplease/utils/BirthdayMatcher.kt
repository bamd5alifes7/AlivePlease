package com.orenhui.aliveplease.utils

import java.util.Calendar

/**
 * 生日日期比對工具。
 * 純函數，無 Android 依賴，所有邏輯均可在 JVM 單元測試中驗證。
 */
object BirthdayMatcher {

    /**
     * 判斷指定年份是否為閏年。
     * 閏年規則：能被 4 整除，但不能被 100 整除；或能被 400 整除。
     */
    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    /**
     * 判斷今天是否為生日觸發日。
     *
     * 規則：
     * - [birthdayMonth] 或 [birthdayDay] 為 0（未設定）時，回傳 false。
     * - 月份需在 1–12 範圍，日期需在 1–31 範圍，否則回傳 false。
     * - 非閏年且生日為 2/29，改在 2/28 觸發。
     * - 其他情況：月份與日期均與 [today] 相符時，回傳 true。
     *
     * @param birthdayMonth 設定的生日月份（1–12），0 表示未設定
     * @param birthdayDay   設定的生日日期（1–31），0 表示未設定
     * @param today         用於比對的今日 Calendar，預設為當下時間（方便測試注入）
     */
    fun isBirthdayToday(
        birthdayMonth: Int,
        birthdayDay: Int,
        today: Calendar = Calendar.getInstance()
    ): Boolean {
        // 未設定
        if (birthdayMonth == 0 || birthdayDay == 0) return false

        // 邊界防禦
        if (birthdayMonth !in 1..12) return false
        if (birthdayDay !in 1..31) return false

        val todayMonth = today.get(Calendar.MONTH) + 1  // Calendar.MONTH 從 0 起算
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val todayYear = today.get(Calendar.YEAR)

        return if (birthdayMonth == 2 && birthdayDay == 29 && !isLeapYear(todayYear)) {
            // 非閏年，2/29 改在 2/28 觸發
            todayMonth == 2 && todayDay == 28
        } else {
            todayMonth == birthdayMonth && todayDay == birthdayDay
        }
    }
}
