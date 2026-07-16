package com.orenhui.aliveplease.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * BirthdayMatcher 單元測試。
 * 純 JVM 測試，不需要 Android 環境或 Robolectric。
 */
class BirthdayMatcherTest {

    // ─────────────────────────────────────────────
    // 輔助函數：建立 Calendar
    // ─────────────────────────────────────────────

    private fun calendarOf(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)  // Calendar.MONTH 從 0 起算
            set(Calendar.DAY_OF_MONTH, day)
        }

    // ─────────────────────────────────────────────
    // isLeapYear
    // ─────────────────────────────────────────────

    @Test
    fun `isLeapYear - 2024 是閏年`() {
        assertTrue(BirthdayMatcher.isLeapYear(2024))
    }

    @Test
    fun `isLeapYear - 2000 是閏年（400年整除）`() {
        assertTrue(BirthdayMatcher.isLeapYear(2000))
    }

    @Test
    fun `isLeapYear - 1900 不是閏年（100年整除但非400年）`() {
        assertFalse(BirthdayMatcher.isLeapYear(1900))
    }

    @Test
    fun `isLeapYear - 2023 不是閏年`() {
        assertFalse(BirthdayMatcher.isLeapYear(2023))
    }

    @Test
    fun `isLeapYear - 1600 是閏年`() {
        assertTrue(BirthdayMatcher.isLeapYear(1600))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 未設定的情況
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 月份為0（未設定）回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(0, 14, today))
    }

    @Test
    fun `isBirthdayToday - 日期為0（未設定）回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(7, 0, today))
    }

    @Test
    fun `isBirthdayToday - 月日均為0（未設定）回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(0, 0, today))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 一般生日
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 生日月日與今日相符回傳 true`() {
        val today = calendarOf(2024, 7, 14)
        assertTrue(BirthdayMatcher.isBirthdayToday(7, 14, today))
    }

    @Test
    fun `isBirthdayToday - 生日月份不同回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(8, 14, today))
    }

    @Test
    fun `isBirthdayToday - 生日日期不同回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(7, 15, today))
    }

    @Test
    fun `isBirthdayToday - 月日均不同回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(12, 25, today))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 2/29 閏年
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 閏年2月29日，當天2月29日回傳 true`() {
        val today = calendarOf(2024, 2, 29)  // 2024 是閏年
        assertTrue(BirthdayMatcher.isBirthdayToday(2, 29, today))
    }

    @Test
    fun `isBirthdayToday - 閏年2月29日，當天2月28日回傳 false`() {
        val today = calendarOf(2024, 2, 28)  // 2024 是閏年
        assertFalse(BirthdayMatcher.isBirthdayToday(2, 29, today))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 2/29 非閏年（改在 2/28 觸發）
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 非閏年2月29日生日，當天2月28日回傳 true`() {
        val today = calendarOf(2023, 2, 28)  // 2023 不是閏年
        assertTrue(BirthdayMatcher.isBirthdayToday(2, 29, today))
    }

    @Test
    fun `isBirthdayToday - 非閏年2月29日生日，當天3月1日回傳 false`() {
        val today = calendarOf(2023, 3, 1)  // 2023 不是閏年
        assertFalse(BirthdayMatcher.isBirthdayToday(2, 29, today))
    }

    @Test
    fun `isBirthdayToday - 非閏年2月29日生日，當天2月27日回傳 false`() {
        val today = calendarOf(2023, 2, 27)
        assertFalse(BirthdayMatcher.isBirthdayToday(2, 29, today))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 月份邊界防禦
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 月份為13（超出）回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(13, 14, today))
    }

    @Test
    fun `isBirthdayToday - 月份為負數回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(-1, 14, today))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 日期邊界防禦
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 日期為32（超出）回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(7, 32, today))
    }

    @Test
    fun `isBirthdayToday - 日期為負數回傳 false`() {
        val today = calendarOf(2024, 7, 14)
        assertFalse(BirthdayMatcher.isBirthdayToday(7, -1, today))
    }

    // ─────────────────────────────────────────────
    // isBirthdayToday - 年底年初跨年情境
    // ─────────────────────────────────────────────

    @Test
    fun `isBirthdayToday - 12月31日生日，當天12月31日回傳 true`() {
        val today = calendarOf(2024, 12, 31)
        assertTrue(BirthdayMatcher.isBirthdayToday(12, 31, today))
    }

    @Test
    fun `isBirthdayToday - 1月1日生日，當天1月1日回傳 true`() {
        val today = calendarOf(2024, 1, 1)
        assertTrue(BirthdayMatcher.isBirthdayToday(1, 1, today))
    }
}
