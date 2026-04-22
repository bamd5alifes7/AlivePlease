package com.example.aliveplease.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
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
}
