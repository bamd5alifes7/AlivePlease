package com.orenhui.aliveplease.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidCheckInTrackerTest {

    @Test
    fun `triggers after ten check ins within one minute`() {
        val tracker = RapidCheckInTracker()

        repeat(9) { index ->
            assertFalse(tracker.record(index * 1_000L))
        }

        assertTrue(tracker.record(9_000L))
    }

    @Test
    fun `does not count check ins older than one minute`() {
        val tracker = RapidCheckInTracker()

        repeat(9) { index ->
            assertFalse(tracker.record(index * 1_000L))
        }

        assertFalse(tracker.record(70_000L))
    }

    @Test
    fun `can trigger again after a completed round`() {
        val tracker = RapidCheckInTracker()

        repeat(9) { index ->
            assertFalse(tracker.record(index.toLong()))
        }
        assertTrue(tracker.record(9L))

        repeat(9) { index ->
            assertFalse(tracker.record(10L + index))
        }
        assertTrue(tracker.record(19L))
    }
}
