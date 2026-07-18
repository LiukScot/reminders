package com.liukscot.reminders.ui.screens

import com.liukscot.reminders.data.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

private val Today: LocalDate = LocalDate.of(2026, 7, 18)

private fun taskAt(time: LocalTime?): Task = Task(
    listId = 1,
    title = "Task",
    dueAt = time?.let { Today.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() },
    hasDueTime = time != null,
    createdAt = 0,
)

private fun Long.asLocalTime(): LocalTime =
    java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime()

class DaySlotDropTest {
    @Test
    fun `dropping a task back in its own slot keeps its time`() {
        val evening = taskAt(LocalTime.of(21, 0))
        assertEquals(LocalTime.of(21, 0), evening.dueAtForSlot(Today, DaySlot.Evening).asLocalTime())
    }

    @Test
    fun `moving a task to another slot takes that slot's hour`() {
        val evening = taskAt(LocalTime.of(21, 0))
        assertEquals(LocalTime.of(9, 0), evening.dueAtForSlot(Today, DaySlot.Morning).asLocalTime())
        assertEquals(LocalTime.of(15, 0), evening.dueAtForSlot(Today, DaySlot.Afternoon).asLocalTime())
    }

    @Test
    fun `moving to another day within the same slot keeps the time`() {
        val evening = taskAt(LocalTime.of(21, 0))
        val tomorrow = Today.plusDays(1)
        val moved = evening.dueAtForSlot(tomorrow, DaySlot.Evening)
        assertEquals(LocalTime.of(21, 0), moved.asLocalTime())
        assertEquals(tomorrow, java.time.Instant.ofEpochMilli(moved).atZone(ZoneId.systemDefault()).toLocalDate())
    }

    @Test
    fun `a task with no time takes the slot's hour`() {
        val undated = taskAt(null)
        assertEquals(LocalTime.of(18, 0), undated.dueAtForSlot(Today, DaySlot.Evening).asLocalTime())
    }
}
