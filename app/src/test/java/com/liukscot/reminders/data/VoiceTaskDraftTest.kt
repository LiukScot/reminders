package com.liukscot.reminders.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONException

class VoiceTaskDraftTest {
    // The bug this guards: without the current time in the prompt, "in 10 minutes" is unresolvable —
    // the model has no "now" to add to, so it invents a time. The instruction must carry it.
    @Test
    fun `instruction carries the current date and time`() {
        val instruction = voiceTaskInstruction(LocalDateTime.of(2026, 7, 17, 19, 5), listOf("Personal"))
        assertTrue(instruction.contains("2026-07-17 19:05"))
        assertTrue(instruction.contains("FRIDAY"))
    }

    @Test
    fun `full draft with date, time and recurrence`() {
        val json = """
            {"title":"gym","date":"2026-07-20","time":"18:30",
             "recurrence":{"frequency":"WEEKLY","interval":1,"byDay":["MONDAY"]}}
        """.trimIndent()
        val draft = parseVoiceTaskDraft(json)
        assertEquals("gym", draft.title)
        assertEquals(LocalDate.of(2026, 7, 20), draft.date)
        assertEquals(LocalTime.of(18, 30), draft.time)
        assertEquals(RecurrenceRule(RecurrenceFrequency.WEEKLY, 1, setOf(DayOfWeek.MONDAY)), draft.recurrence)
    }

    @Test
    fun `title only, nulls for the rest`() {
        val draft = parseVoiceTaskDraft("""{"title":"buy milk","date":null,"time":null,"recurrence":null}""")
        assertEquals("buy milk", draft.title)
        assertNull(draft.date)
        assertNull(draft.time)
        assertNull(draft.recurrence)
    }

    @Test
    fun `omitted fields are treated as absent`() {
        val draft = parseVoiceTaskDraft("""{"title":"call mom"}""")
        assertEquals("call mom", draft.title)
        assertNull(draft.date)
        assertNull(draft.time)
        assertNull(draft.recurrence)
        assertNull(draft.list)
    }

    @Test
    fun `list field is read when the reminder names a list`() {
        val draft = parseVoiceTaskDraft("""{"title":"pasta","list":"spesa"}""")
        assertEquals("pasta", draft.title)
        assertEquals("spesa", draft.list)
    }

    @Test
    fun `a malformed optional field degrades to null without losing the title`() {
        val draft = parseVoiceTaskDraft("""{"title":"dentist","date":"not-a-date","time":"25:99"}""")
        assertEquals("dentist", draft.title)
        assertNull(draft.date)
        assertNull(draft.time)
    }

    @Test
    fun `interval defaults to at least 1`() {
        val draft = parseVoiceTaskDraft("""{"title":"x","recurrence":{"frequency":"DAILY"}}""")
        assertEquals(RecurrenceRule(RecurrenceFrequency.DAILY, 1), draft.recurrence)
    }

    @Test
    fun `non-JSON reply throws so the caller can surface a failed call`() {
        assertThrows(JSONException::class.java) { parseVoiceTaskDraft("sorry, I couldn't hear that") }
    }
}
