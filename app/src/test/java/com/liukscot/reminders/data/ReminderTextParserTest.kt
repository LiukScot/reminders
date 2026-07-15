package com.liukscot.reminders.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTextParserTest {
    // Wednesday, so weekday-name tests below have an unambiguous "next occurrence".
    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `Italian date and time, acceptance criteria example`() {
        val result = parseReminderText("dentista domani alle 15", today)
        assertEquals("dentista", result.cleanTitle)
        assertEquals(today.plusDays(1), result.date)
        assertEquals(LocalTime.of(15, 0), result.time)
        assertNull(result.recurrence)
    }

    @Test
    fun `Italian weekly recurrence, acceptance criteria example`() {
        val result = parseReminderText("palestra ogni lunedì", today)
        assertEquals("palestra", result.cleanTitle)
        assertEquals(RecurrenceRule(RecurrenceFrequency.WEEKLY, 1, setOf(DayOfWeek.MONDAY)), result.recurrence)
        // No explicit date word: anchors on the next Monday from today (Wed 2026-07-15).
        assertEquals(LocalDate.of(2026, 7, 20), result.date)
    }

    @Test
    fun `Italian today with explicit time`() {
        val result = parseReminderText("spesa oggi alle 18:30", today)
        assertEquals("spesa", result.cleanTitle)
        assertEquals(today, result.date)
        assertEquals(LocalTime.of(18, 30), result.time)
    }

    @Test
    fun `Italian day after tomorrow`() {
        val result = parseReminderText("dopodomani chiamare il commercialista", today)
        assertEquals("chiamare il commercialista", result.cleanTitle)
        assertEquals(today.plusDays(2), result.date)
    }

    @Test
    fun `Italian interval recurrence`() {
        val result = parseReminderText("pagare bollette ogni 2 settimane", today)
        assertEquals("pagare bollette", result.cleanTitle)
        assertEquals(RecurrenceRule(RecurrenceFrequency.WEEKLY, 2), result.recurrence)
    }

    @Test
    fun `English date and time`() {
        val result = parseReminderText("dentist tomorrow at 3pm", today)
        assertEquals("dentist", result.cleanTitle)
        assertEquals(today.plusDays(1), result.date)
        assertEquals(LocalTime.of(15, 0), result.time)
    }

    @Test
    fun `English weekly recurrence`() {
        val result = parseReminderText("gym every monday", today)
        assertEquals("gym", result.cleanTitle)
        assertEquals(RecurrenceRule(RecurrenceFrequency.WEEKLY, 1, setOf(DayOfWeek.MONDAY)), result.recurrence)
    }

    @Test
    fun `English shorthand recurrence word`() {
        val result = parseReminderText("water the plants daily", today)
        assertEquals("water the plants", result.cleanTitle)
        assertEquals(RecurrenceRule(RecurrenceFrequency.DAILY, 1), result.recurrence)
    }

    @Test
    fun `English today with am time`() {
        val result = parseReminderText("standup today at 9am", today)
        assertEquals("standup", result.cleanTitle)
        assertEquals(today, result.date)
        assertEquals(LocalTime.of(9, 0), result.time)
    }

    @Test
    fun `plain title with no natural language phrase is left untouched`() {
        val result = parseReminderText("compra il latte", today)
        assertEquals("compra il latte", result.cleanTitle)
        assertNull(result.date)
        assertNull(result.time)
        assertNull(result.recurrence)
    }

    @Test
    fun `bare weekday name resolves to that date`() {
        // today is Wednesday 2026-07-15; "venerdì" (Friday) is later the same week.
        val result = parseReminderText("consegna venerdì", today)
        assertEquals("consegna", result.cleanTitle)
        assertEquals(LocalDate.of(2026, 7, 17), result.date)
    }

    @Test
    fun `time-of-day words resolve to slot hours, Italian and English`() {
        val morning = parseReminderText("colazione domani mattina", today)
        assertEquals(LocalTime.of(9, 0), morning.time)
        assertEquals(today.plusDays(1), morning.date)
        assertEquals("colazione", morning.cleanTitle)
        assertEquals(LocalTime.of(15, 0), parseReminderText("call tomorrow afternoon", today).time)
        assertEquals(LocalTime.of(18, 0), parseReminderText("cena sera", today).time)
    }

    @Test
    fun `contracted today words resolve date and time together`() {
        val stasera = parseReminderText("cena stasera", today)
        assertEquals("cena", stasera.cleanTitle)
        assertEquals(today, stasera.date)
        assertEquals(LocalTime.of(18, 0), stasera.time)

        val stamattina = parseReminderText("palestra stamattina", today)
        assertEquals(today, stamattina.date)
        assertEquals(LocalTime.of(9, 0), stamattina.time)

        val tonight = parseReminderText("dinner tonight", today)
        assertEquals(today, tonight.date)
        assertEquals(LocalTime.of(18, 0), tonight.time)

        val thisAfternoon = parseReminderText("meeting this afternoon", today)
        assertEquals("meeting", thisAfternoon.cleanTitle)
        assertEquals(today, thisAfternoon.date)
        assertEquals(LocalTime.of(15, 0), thisAfternoon.time)
    }

    @Test
    fun `spelled-out hours, English and Italian`() {
        // The exact case from the screenshot — "o clock" with no apostrophe.
        val en = parseReminderText("tomorrow at sixteen o clock", today)
        assertEquals(today.plusDays(1), en.date)
        assertEquals(LocalTime.of(16, 0), en.time)
        assertEquals("", en.cleanTitle)

        assertEquals(LocalTime.of(9, 0), parseReminderText("call at nine", today).time)
        assertEquals(LocalTime.of(21, 0), parseReminderText("dinner at nine pm", today).time)
        assertEquals(LocalTime.of(23, 0), parseReminderText("meeting at twenty-three", today).time)
        assertEquals(LocalTime.of(16, 0), parseReminderText("riunione alle sedici", today).time)
        assertEquals("riunione", parseReminderText("riunione alle sedici", today).cleanTitle)
        // o'clock with an apostrophe, no "at".
        assertEquals(LocalTime.of(15, 0), parseReminderText("call three o'clock pm", today).time)
    }

    @Test
    fun `fractional spelled times, English`() {
        // The second screenshot case.
        val half = parseReminderText("test tomorrow thirteen and a half", today)
        assertEquals("test", half.cleanTitle)
        assertEquals(today.plusDays(1), half.date)
        assertEquals(LocalTime.of(13, 30), half.time)

        assertEquals(LocalTime.of(4, 30), parseReminderText("meet half past four", today).time)
        assertEquals(LocalTime.of(4, 15), parseReminderText("meet quarter past four", today).time)
        assertEquals(LocalTime.of(1, 45), parseReminderText("meet quarter to two", today).time)
        assertEquals(LocalTime.of(4, 15), parseReminderText("call four and a quarter", today).time)
        assertEquals(LocalTime.of(12, 0), parseReminderText("lunch at noon", today).time)
    }

    @Test
    fun `fractional spelled times, Italian`() {
        assertEquals(LocalTime.of(13, 30), parseReminderText("pranzo tredici e mezza", today).time)
        assertEquals(LocalTime.of(8, 15), parseReminderText("sveglia otto e un quarto", today).time)
        assertEquals(LocalTime.of(9, 45), parseReminderText("call nove e tre quarti", today).time)
        assertEquals(LocalTime.of(12, 0), parseReminderText("pranzo a mezzogiorno", today).time)
        assertEquals(LocalTime.of(15, 30), parseReminderText("riunione alle 15:30", today).time)
    }

    @Test
    fun `at without a number word is left in the title`() {
        // "at home" must not be swallowed as a time.
        val result = parseReminderText("meet at home", today)
        assertEquals("meet at home", result.cleanTitle)
        assertNull(result.time)
    }

    @Test
    fun `matched ranges point at the recognized phrases in the original text`() {
        val raw = "dentista domani alle 15"
        val result = parseReminderText(raw, today)
        // The highlight relies on these ranges being in original-string coordinates.
        val matched = result.matchedRanges.map { raw.substring(it.first, it.last + 1) }.toSet()
        assertEquals(setOf("domani", "alle 15"), matched)
    }
}
