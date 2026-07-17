package com.liukscot.reminders.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeLabelTest {
    @Test
    fun `compact label is minutes under an hour and whole hours at or above`() {
        assertEquals("10m", compactSnoozeLabel(10))
        assertEquals("15m", compactSnoozeLabel(15))
        assertEquals("1h", compactSnoozeLabel(60))
        assertEquals("2h", compactSnoozeLabel(120))
    }
}
