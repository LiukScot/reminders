package com.liukscot.reminders.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId"), Index("parentId")],
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val parentId: Long? = null,
    val title: String,
    val notes: String? = null,
    val dueAt: Long? = null,
    val hasDueTime: Boolean = false,
    val flagged: Boolean = false,
    val priority: Int = 0,
    val completed: Boolean = false,
    val createdAt: Long,
    // Recurrence: null recurrenceFreq means "does not repeat". recurrenceAnchor is the fixed
    // date/time the cadence is computed from — rescheduling changes dueAt only, so the rule
    // keeps ticking on its original schedule (e.g. "every Thursday" stays on Thursday even if
    // one occurrence gets moved to Friday). See RecurrenceRule.kt.
    val recurrenceFreq: String? = null,
    val recurrenceInterval: Int = 1,
    val recurrenceByDay: String? = null,
    val recurrenceAnchor: Long? = null,
)
