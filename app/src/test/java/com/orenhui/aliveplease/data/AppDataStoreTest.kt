package com.orenhui.aliveplease.data

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppDataStoreTest {

    private lateinit var dataStore: AppDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().build()
        )
        context.getSharedPreferences("alive_please_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        dataStore = AppDataStore(context)
    }

    @Test
    fun shouldScheduleFamilyNotification_isFalseBeforeAnyCheckIn() {
        dataStore.setFamilyEmail("family@example.com")

        assertFalse(dataStore.shouldScheduleFamilyNotification())
    }

    @Test
    fun shouldScheduleFamilyNotification_isFalseWithoutRecipient() {
        dataStore.performCheckIn()

        assertFalse(dataStore.shouldScheduleFamilyNotification())
    }

    @Test
    fun shouldScheduleFamilyNotification_isTrueAfterCheckInAndRecipientConfigured() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.performCheckIn()

        assertTrue(dataStore.shouldScheduleFamilyNotification())
    }

    @Test
    fun successfulRecipient_isNotScheduledAgainDuringSameCheckInCycle() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.performCheckIn()

        dataStore.markFamilyNotificationSent(
            dataStore.getFamilyNotificationCycleId(),
            "family@example.com"
        )
        val reopenedDataStore = AppDataStore(
            ApplicationProvider.getApplicationContext<android.content.Context>()
        )

        assertFalse(reopenedDataStore.shouldScheduleFamilyNotification())
        assertTrue(reopenedDataStore.getPendingFamilyNotificationContacts().isEmpty())
    }

    @Test
    fun partialSuccess_onlyLeavesFailedRecipientsPending() {
        dataStore.setFamilyContacts(
            listOf(
                FamilyContact("媽媽", "mom@example.com"),
                FamilyContact("哥哥", "brother@example.com")
            )
        )
        dataStore.performCheckIn()

        dataStore.markFamilyNotificationSent(
            dataStore.getFamilyNotificationCycleId(),
            "mom@example.com"
        )

        assertEquals(
            listOf(FamilyContact("哥哥", "brother@example.com")),
            dataStore.getPendingFamilyNotificationContacts()
        )
    }

    @Test
    fun newCheckIn_startsNewFamilyNotificationCycle() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.performCheckIn()
        dataStore.markFamilyNotificationSent(
            dataStore.getFamilyNotificationCycleId(),
            "family@example.com"
        )

        dataStore.performCheckIn()

        assertTrue(dataStore.shouldScheduleFamilyNotification())
        assertEquals(1, dataStore.getPendingFamilyNotificationContacts().size)
    }

    @Test
    fun familyNotificationRequestId_isStableForSameCycleAndRecipient() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.performCheckIn()
        val cycleId = dataStore.getFamilyNotificationCycleId()

        val firstRequestId = dataStore.getFamilyNotificationRequestId(cycleId, "FAMILY@example.com")
        val secondRequestId = dataStore.getFamilyNotificationRequestId(cycleId, "family@example.com")

        assertEquals(firstRequestId, secondRequestId)
    }

    @Test
    fun staleNotificationResult_doesNotMarkNewCheckInCycleAsSent() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.performCheckIn()
        val oldCycleId = dataStore.getFamilyNotificationCycleId()
        dataStore.setLastCheckInTime(oldCycleId + 1)

        val marked = dataStore.markFamilyNotificationSent(oldCycleId, "family@example.com")

        assertFalse(marked)
        assertTrue(dataStore.shouldScheduleFamilyNotification())
    }

    @Test
    fun getFamilyContacts_fallsBackToLegacyRecipientSettings() {
        dataStore.setFamilyRecipientTitle("媽媽")
        dataStore.setFamilyEmail("mom@example.com")

        val contacts = dataStore.getFamilyContacts()

        assertEquals(listOf(FamilyContact("媽媽", "mom@example.com")), contacts)
    }

    @Test
    fun setFamilyContacts_persistsMultipleRecipients() {
        val contacts = listOf(
            FamilyContact("媽媽", "mom@example.com"),
            FamilyContact("哥哥", "brother@example.com")
        )

        dataStore.setFamilyContacts(contacts)

        assertEquals(contacts, dataStore.getFamilyContacts())
        assertTrue(dataStore.hasFamilyNotificationRecipient())
    }

    @Test
    fun quietHours_defaultsToEnabledFrom2300To0700() {
        assertTrue(dataStore.isQuietHoursEnabled())
        assertEquals(23 * 60, dataStore.getQuietHoursStartMinutes())
        assertEquals(7 * 60, dataStore.getQuietHoursEndMinutes())
    }

    @Test
    fun familyWarningBeforeHours_defaultsToHalfHour() {
        assertEquals(0.5f, dataStore.getFamilyWarningBeforeHours(), 0.001f)
    }

    @Test
    fun setFirstLaunchCompleted_persistsCompletedStateImmediately() {
        assertTrue(dataStore.isFirstLaunch())

        dataStore.setFirstLaunchCompleted()

        assertFalse(dataStore.isFirstLaunch())
    }

    @Test
    fun consumeSetupTutorialPending_returnsTrueOnce() {
        assertFalse(dataStore.consumeSetupTutorialPending())

        dataStore.setSetupTutorialPending()

        assertTrue(dataStore.consumeSetupTutorialPending())
        assertFalse(dataStore.consumeSetupTutorialPending())
    }

    @Test
    fun shouldSendCheckInReminder_isTrueBeforeAnyCheckIn() {
        assertTrue(dataStore.shouldSendCheckInReminder())
    }

    @Test
    fun shouldSendCheckInReminder_isFalseUntilNotifyIntervalPasses() {
        dataStore.setNotifyInterval(12)
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (30 * 60 * 1000L))

        assertFalse(dataStore.shouldSendCheckInReminder())
        assertTrue(dataStore.getTimeUntilCheckInReminder() in (11 * 60 * 60 * 1000L)..(12 * 60 * 60 * 1000L))
    }

    @Test
    fun shouldSendCheckInReminder_isTrueAfterNotifyIntervalPasses() {
        dataStore.setNotifyInterval(1)
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (61 * 60 * 1000L))

        assertTrue(dataStore.shouldSendCheckInReminder())
        assertEquals(0L, dataStore.getTimeUntilCheckInReminder())
    }

    @Test
    fun getTimeUntilFamilyWarning_returnsNotificationDelayMinusWarningWindow() {
        val now = System.currentTimeMillis()
        dataStore.setLastCheckInTime(now)
        dataStore.setFamilyNotifyIntervalFloat(2f)
        dataStore.setFamilyWarningBeforeHours(0.5f)

        val remaining = dataStore.getTimeUntilFamilyWarning()

        assertTrue(remaining in (89 * 60 * 1000L)..(90 * 60 * 1000L))
    }

    @Test
    fun shouldSendFamilyWarning_isTrueInsideWarningWindow() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.setFamilyNotifyIntervalFloat(2f)
        dataStore.setFamilyWarningBeforeHours(0.5f)
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (95 * 60 * 1000L))

        assertTrue(dataStore.shouldSendFamilyWarning())
    }

    @Test
    fun shouldSendFamilyWarning_isFalseBeforeWarningWindow() {
        dataStore.setFamilyEmail("family@example.com")
        dataStore.setFamilyNotifyIntervalFloat(2f)
        dataStore.setFamilyWarningBeforeHours(0.5f)
        dataStore.setLastCheckInTime(System.currentTimeMillis() - (60 * 60 * 1000L))

        assertFalse(dataStore.shouldSendFamilyWarning())
    }
}
