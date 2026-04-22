package com.example.aliveplease.notifications

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.aliveplease.utils.WorkSchedulerHelper
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
class BootReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        receiver = BootReceiver()
        mockkObject(WorkSchedulerHelper)
        every { WorkSchedulerHelper.rescheduleAll(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onReceive_reschedulesAfterBootCompleted() {
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        verify(exactly = 1) { WorkSchedulerHelper.rescheduleAll(context) }
    }

    @Test
    fun onReceive_reschedulesAfterPackageReplaced() {
        receiver.onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        verify(exactly = 1) { WorkSchedulerHelper.rescheduleAll(context) }
    }

    @Test
    fun onReceive_ignoresUnrelatedActions() {
        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

        verify(exactly = 0) { WorkSchedulerHelper.rescheduleAll(any()) }
    }
}
