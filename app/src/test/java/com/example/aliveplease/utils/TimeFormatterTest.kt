package com.example.aliveplease.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TimeFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun formatCountdown_includesSecondsWhenOverOneDay() {
        val milliseconds = (((1L * 24 + 2) * 60 + 3) * 60 + 4) * 1000

        assertEquals("1 天 2 小時 3 分鐘 4 秒", TimeFormatter.formatCountdown(context, milliseconds))
    }

    @Test
    fun formatCountdown_keepsZeroUnitsWhenOverOneDay() {
        val milliseconds = 1L * 24 * 60 * 60 * 1000

        assertEquals("1 天 0 小時 0 分鐘 0 秒", TimeFormatter.formatCountdown(context, milliseconds))
    }
}
