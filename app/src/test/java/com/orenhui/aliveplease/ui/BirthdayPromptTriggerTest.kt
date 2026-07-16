package com.orenhui.aliveplease.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayPromptTriggerTest {

    @Test
    fun `shows only once on the birthday`() {
        assertTrue(shouldShowBirthdayPrompt(true, "", "2026-07-17"))
        assertFalse(shouldShowBirthdayPrompt(true, "2026-07-17", "2026-07-17"))
        assertFalse(shouldShowBirthdayPrompt(false, "", "2026-07-17"))
    }
}
