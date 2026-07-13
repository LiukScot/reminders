package com.liukscot.reminders.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TaskTagRow(val taskId: Long, val name: String)

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun findByName(name: String): Tag?

    @Insert
    suspend fun insert(tag: Tag): Long

    @Query(
        "SELECT task_tag_cross_ref.taskId AS taskId, tags.name AS name " +
            "FROM task_tag_cross_ref JOIN tags ON tags.id = task_tag_cross_ref.tagId " +
            "ORDER BY tags.name ASC",
    )
    fun tagsByTaskId(): Flow<List<TaskTagRow>>

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun clearTagsForTask(taskId: Long)

    @Insert
    suspend fun insertCrossRefs(refs: List<TaskTagCrossRef>)

    @Query(
        "SELECT tasks.* FROM tasks " +
            "JOIN task_tag_cross_ref ON task_tag_cross_ref.taskId = tasks.id " +
            "JOIN tags ON tags.id = task_tag_cross_ref.tagId " +
            "WHERE tags.name = :tagName ORDER BY tasks.createdAt DESC",
    )
    fun tasksByTagName(tagName: String): Flow<List<Task>>
}
