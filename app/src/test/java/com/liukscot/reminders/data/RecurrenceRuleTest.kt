package com.liukscot.reminders.data

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecurrenceRuleTest {
    private val zone = ZoneId.of("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `rescheduling one occurrence does not shift the weekly recurrence rule`() {
        // Anchor: Thursday 2026-07-16 10:00 — "every Thursday at 10am".
        val anchor = at(2026, 7, 16)
        val rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, interval = 1)

        // This occurrence gets manually moved to Friday — only dueAt changes, mirroring what
        // the reschedule paths (ReminderSheet edit, Day/Week drag&drop) actually do: they
        // `task.copy(dueAt = ...)` and never touch recurrenceAnchor.
        val rescheduledDueAt = at(2026, 7, 17)
        assertNotEquals(anchor, rescheduledDueAt)

        // The next occurrence must still follow the ORIGINAL Thursday cadence, not Friday.
        val next = nextOccurrence(anchor, rule, zone)
        assertEquals(at(2026, 7, 23), next)
    }

    @Test
    fun `daily recurrence advances by the interval`() {
        val anchor = at(2026, 7, 16)
        val rule = RecurrenceRule(RecurrenceFrequency.DAILY, interval = 3)
        assertEquals(at(2026, 7, 19), nextOccurrence(anchor, rule, zone))
    }

    @Test
    fun `monthly recurrence advances by the interval preserving day of month`() {
        val anchor = at(2026, 1, 31)
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, interval = 1)
        // java.time clamps Feb 31 -> Feb 28 (2026 is not a leap year), same as Task.dueAt would.
        assertEquals(at(2026, 2, 28), nextOccurrence(anchor, rule, zone))
    }

    @Test
    fun `yearly recurrence advances by the interval`() {
        val anchor = at(2026, 7, 16)
        val rule = RecurrenceRule(RecurrenceFrequency.YEARLY, interval = 1)
        assertEquals(at(2027, 7, 16), nextOccurrence(anchor, rule, zone))
    }

    @Test
    fun `weekly byDay finds the next matching weekday within the same interval week`() {
        // Anchor: Monday 2026-07-13, "every week on Mon, Wed, Fri" — next match after Monday
        // is Wednesday the same week.
        val anchor = at(2026, 7, 13)
        val rule = RecurrenceRule(
            RecurrenceFrequency.WEEKLY,
            interval = 1,
            byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )
        assertEquals(at(2026, 7, 15), nextOccurrence(anchor, rule, zone))
    }

    @Test
    fun `weekly byDay with interval 2 skips the off week`() {
        // Anchor: Friday 2026-07-17 (last byDay of week 0) — next match is Monday of week 2,
        // not week 1, because interval=2 skips the odd week.
        val anchor = at(2026, 7, 17)
        val rule = RecurrenceRule(
            RecurrenceFrequency.WEEKLY,
            interval = 2,
            byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )
        assertEquals(at(2026, 7, 27), nextOccurrence(anchor, rule, zone))
    }
}
