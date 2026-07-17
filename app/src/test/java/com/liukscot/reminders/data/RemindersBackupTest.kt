package com.liukscot.reminders.data

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemindersBackupTest {
    @Test
    fun encodeThenDecode_preservesEveryField() {
        val backup = RemindersBackup(
            lists = listOf(TaskList(id = 1, name = "Personal", icon = "inbox")),
            tags = listOf(Tag(id = 7, name = "work"), Tag(id = 8, name = "urgent")),
            tasks = listOf(
                Task(
                    id = 42,
                    listId = 1,
                    title = "Call the plumber",
                    notes = "about the leak",
                    dueAt = 1_700_000_000_000,
                    hasDueTime = true,
                    flagged = true,
                    priority = 2,
                    completed = false,
                    createdAt = 1_699_000_000_000,
                    recurrenceFreq = "WEEKLY",
                    recurrenceInterval = 2,
                    recurrenceByDay = "TH",
                    recurrenceAnchor = 1_700_000_000_000,
                ),
            ),
            tagIdsByTaskId = mapOf(42L to listOf(7L, 8L)),
        )

        val decoded = decodeBackup(encodeBackup(backup, exportedAt = 0))

        assertEquals(backup.lists, decoded.lists)
        assertEquals(backup.tags, decoded.tags)
        assertEquals(backup.tasks, decoded.tasks)
        assertEquals(backup.tagIdsByTaskId, decoded.tagIdsByTaskId)
    }

    // Every nullable column has to survive as null rather than come back as the string "null" or
    // blow up on the way out — an unset due date is the common case, not an edge case.
    @Test
    fun encodeThenDecode_keepsUnsetFieldsNull() {
        val backup = RemindersBackup(
            lists = listOf(TaskList(id = 1, name = "Personal", icon = "inbox")),
            tags = emptyList(),
            tasks = listOf(Task(id = 1, listId = 1, title = "Someday", createdAt = 5)),
            tagIdsByTaskId = emptyMap(),
        )

        val task = decodeBackup(encodeBackup(backup, exportedAt = 0)).tasks.single()

        assertNull(task.notes)
        assertNull(task.dueAt)
        assertNull(task.parentId)
        assertNull(task.recurrenceFreq)
        assertNull(task.recurrenceByDay)
        assertNull(task.recurrenceAnchor)
    }

    @Test
    fun decode_rejectsBackupFromANewerVersion() {
        val text = encodeBackup(emptyBackup(), exportedAt = 0)
            .let { JSONObject(it).put("version", BACKUP_FORMAT_VERSION + 1).toString() }

        val error = runCatching { decodeBackup(text) }.exceptionOrNull()

        assertTrue("expected a rejection, got $error", error is IllegalArgumentException)
    }

    @Test
    fun decode_rejectsAFileThatIsNotJson() {
        val error = runCatching { decodeBackup("this is a photo, not a backup") }.exceptionOrNull()

        assertTrue("expected a rejection, got $error", error is JSONException)
    }

    private fun emptyBackup() = RemindersBackup(emptyList(), emptyList(), emptyList(), emptyMap())
}
