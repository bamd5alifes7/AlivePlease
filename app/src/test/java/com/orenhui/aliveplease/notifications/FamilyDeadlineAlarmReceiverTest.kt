package com.orenhui.aliveplease.notifications

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.utils.WorkSchedulerHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FamilyDeadlineAlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var dataStore: AppDataStore
    private lateinit var receiver: FamilyDeadlineAlarmReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("alive_please_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        dataStore = AppDataStore(context)
        receiver = FamilyDeadlineAlarmReceiver()

        mockkObject(NotificationHelper)
        mockkObject(WorkSchedulerHelper)
        every { NotificationHelper.createNotificationChannels(any()) } just runs
        every { NotificationHelper.sendFamilyDeadlineAlert(any()) } just runs
        every { WorkSchedulerHelper.enqueueFamilyNotificationNow(any()) } just runs
        every { WorkSchedulerHelper.scheduleFamilyNotification(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onReceive_sendsLocalAlertAndEnqueuesMailWhenDeadlinePassed() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (29 * 60 * 60 * 1000L))

        receiver.onReceive(context, familyDeadlineIntent())

        verify(exactly = 1) { NotificationHelper.sendFamilyDeadlineAlert(context) }
        verify(exactly = 1) { WorkSchedulerHelper.enqueueFamilyNotificationNow(context) }
        verify(exactly = 0) { WorkSchedulerHelper.scheduleFamilyNotification(any(), any()) }
    }

    @Test
    fun onReceive_reschedulesWhenDeadlineHasNotPassed() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (27 * 60 * 60 * 1000L))

        receiver.onReceive(context, familyDeadlineIntent())

        verify(exactly = 0) { NotificationHelper.sendFamilyDeadlineAlert(any()) }
        verify(exactly = 0) { WorkSchedulerHelper.enqueueFamilyNotificationNow(any()) }
        verify(exactly = 1) { WorkSchedulerHelper.scheduleFamilyNotification(context, any()) }
    }

    @Test
    fun onReceive_ignoresUnrelatedActions() {
        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

        verify(exactly = 0) { NotificationHelper.sendFamilyDeadlineAlert(any()) }
        verify(exactly = 0) { WorkSchedulerHelper.enqueueFamilyNotificationNow(any()) }
    }

    private fun familyDeadlineIntent(): Intent {
        return Intent(FamilyDeadlineAlarmReceiver.ACTION_FAMILY_DEADLINE_ALARM)
    }
}
