package com.orenhui.aliveplease.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.notifications.NotificationHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FamilyWarningWorkerTest {

    private lateinit var context: Context
    private lateinit var dataStore: AppDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("alive_please_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        dataStore = AppDataStore(context)
        mockkObject(NotificationHelper)
        every { NotificationHelper.sendFamilyWarning(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun doWork_doesNotNotifyOutsideWarningWindow() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.setFamilyNotifyIntervalFloat(2f)
        dataStore.setFamilyWarningBeforeHours(0.5f)
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (60 * 60 * 1000L))

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(exactly = 0) { NotificationHelper.sendFamilyWarning(any(), any()) }
    }

    @Test
    fun doWork_sendsWarningInsideWarningWindow() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.setFamilyNotifyIntervalFloat(2f)
        dataStore.setFamilyWarningBeforeHours(0.5f)
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (95 * 60 * 1000L))

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(exactly = 1) { NotificationHelper.sendFamilyWarning(context, any()) }
    }

    private fun buildWorker(): FamilyWarningWorker {
        return TestListenableWorkerBuilder<FamilyWarningWorker>(context).build()
    }
}
