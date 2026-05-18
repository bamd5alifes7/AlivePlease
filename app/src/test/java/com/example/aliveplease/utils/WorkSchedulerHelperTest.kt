package com.example.aliveplease.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.aliveplease.data.AppDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WorkSchedulerHelperTest {

    private lateinit var dataStore: AppDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("alive_please_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        dataStore = AppDataStore(context)
    }

    @Test
    fun isInQuietHours_handlesDefaultCrossMidnightWindow() {
        assertFalse(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(22, 59)))
        assertTrue(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(23, 0)))
        assertTrue(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(6, 59)))
        assertFalse(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(7, 0)))
    }

    @Test
    fun isInQuietHours_handlesSameDayWindow() {
        dataStore.setQuietHoursStartMinutes(13 * 60)
        dataStore.setQuietHoursEndMinutes(14 * 60)

        assertFalse(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(12, 59)))
        assertTrue(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(13, 0)))
        assertTrue(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(13, 59)))
        assertFalse(WorkSchedulerHelper.isInQuietHours(dataStore, millisAt(14, 0)))
    }

    @Test
    fun adjustForQuietHours_doesNotAdjustWhenDisabled() {
        dataStore.setQuietHoursEnabled(false)
        val triggerAtMillis = millisAt(23, 30)

        assertEquals(triggerAtMillis, WorkSchedulerHelper.adjustForQuietHours(dataStore, triggerAtMillis))
    }

    @Test
    fun adjustForQuietHours_movesCareTriggerToQuietEnd() {
        val triggerAtMillis = millisAt(23, 30)
        val adjusted = WorkSchedulerHelper.adjustForQuietHours(dataStore, triggerAtMillis)

        assertEquals(millisAt(7, 0, dayOffset = 1), adjusted)
    }

    @Test
    fun equalStartAndEndMeansAllDayQuietAndDefersToNextMatchingTime() {
        dataStore.setQuietHoursStartMinutes(7 * 60)
        dataStore.setQuietHoursEndMinutes(7 * 60)
        val triggerAtMillis = millisAt(8, 15)

        assertTrue(WorkSchedulerHelper.isInQuietHours(dataStore, triggerAtMillis))
        assertEquals(millisAt(7, 0, dayOffset = 1), WorkSchedulerHelper.adjustForQuietHours(dataStore, triggerAtMillis))
    }

    private fun millisAt(hour: Int, minute: Int, dayOffset: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 17)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }.timeInMillis
    }
}
