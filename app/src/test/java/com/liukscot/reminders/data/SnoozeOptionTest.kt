package com.liukscot.reminders.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class SnoozeOptionTest {
    @Test
    fun later_mapsEachTimeBand() {
        // 04:00–08:59 -> 15:00 today
        assertEquals(at(2026, 7, 17, 15, 0), laterFrom(at(2026, 7, 17, 6, 30)))
        // 09:00–14:59 -> 18:00 today
        assertEquals(at(2026, 7, 17, 18, 0), laterFrom(at(2026, 7, 17, 9, 0)))
        // 15:00–17:59 -> 21:00 today (the band the old spec left uncovered)
        assertEquals(at(2026, 7, 17, 21, 0), laterFrom(at(2026, 7, 17, 16, 30)))
        // 18:00–23:59 -> 09:00 tomorrow
        assertEquals(at(2026, 7, 18, 9, 0), laterFrom(at(2026, 7, 17, 22, 0)))
    }

    @Test
    fun later_afterMidnightIsStillTheSameMorning() {
        // 02:00 belongs to the small hours of the 17th, so its next morning is the 17th, not the 18th.
        assertEquals(at(2026, 7, 17, 9, 0), laterFrom(at(2026, 7, 17, 2, 0)))
    }

    @Test
    fun later_isAlwaysInTheFuture() {
        // Walk every hour of a day; each band must resolve to a strictly later instant.
        for (hour in 0..23) {
            val now = at(2026, 7, 17, hour, 0)
            assertTrue("hour $hour did not move forward", laterFrom(now).isAfter(now))
        }
    }

    @Test
    fun weekend_isSaturdayFromAWeekday() {
        // 2026-07-17 is a Friday -> the next day, Saturday the 18th, keeping the time.
        assertEquals(at(2026, 7, 18, 14, 30), weekendFrom(at(2026, 7, 17, 14, 30)))
    }

    @Test
    fun weekend_skipsToNextSaturdayWhenAlreadyTheWeekend() {
        // Saturday the 18th -> the Saturday after, the 25th, not today.
        assertEquals(at(2026, 7, 25, 10, 0), weekendFrom(at(2026, 7, 18, 10, 0)))
        // Sunday the 19th -> Saturday the 25th.
        assertEquals(at(2026, 7, 25, 10, 0), weekendFrom(at(2026, 7, 19, 10, 0)))
    }

    @Test
    fun choices_hideTheQuickSnoozeWhenItEqualsAFixedOption() {
        // Quick snooze left at 1h duplicates the +1h option, so it drops out.
        val choices = snoozeChoices(at(2026, 7, 17, 10, 0), Duration.ofHours(1))
        assertEquals(1, choices.count { it.kind == SnoozeKind.Plus1h })
        assertFalse(choices.any { it.kind == SnoozeKind.Quick })
    }

    @Test
    fun choices_keepTheQuickSnoozeWhenItIsDistinct() {
        val choices = snoozeChoices(at(2026, 7, 17, 10, 0), Duration.ofMinutes(45))
        val quick = choices.single { it.kind == SnoozeKind.Quick }
        assertEquals(at(2026, 7, 17, 10, 45), quick.at)
    }

    @Test
    fun choices_hideTomorrowAndWeekendDuplicateOnAFriday() {
        // Friday: "tomorrow" and "this weekend" both land on Saturday. Tomorrow comes first and wins.
        val choices = snoozeChoices(at(2026, 7, 17, 14, 0), Duration.ofMinutes(30))
        assertTrue(choices.any { it.kind == SnoozeKind.Tomorrow })
        assertFalse(choices.any { it.kind == SnoozeKind.Weekend })
    }

    @Test
    fun choices_alwaysEndWithPickDateTime() {
        val choices = snoozeChoices(at(2026, 7, 17, 10, 0), Duration.ofMinutes(30))
        assertEquals(SnoozeKind.PickDateTime, choices.last().kind)
        assertEquals(null, choices.last().at)
    }

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi)
}
