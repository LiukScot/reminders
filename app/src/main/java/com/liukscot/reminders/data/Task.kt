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
    val flagged: Boolean = false,
    val completed: Boolean = false,
    val createdAt: Long,
)
