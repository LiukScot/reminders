package com.liukscot.reminders.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TaskTagName(val taskId: Long, val tagName: String)

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun findByName(name: String): Tag?

    @Insert
    suspend fun insert(tag: Tag): Long

    @Query(
        "SELECT task_tag_cross_ref.taskId AS taskId, tags.name AS tagName " +
            "FROM task_tag_cross_ref " +
            "INNER JOIN tags ON tags.id = task_tag_cross_ref.tagId " +
            "WHERE task_tag_cross_ref.taskId IN (SELECT id FROM tasks WHERE listId = :listId)",
    )
    fun tagNamesForList(listId: Long): Flow<List<TaskTagName>>

    @Query(
        "SELECT tasks.* FROM tasks " +
            "INNER JOIN task_tag_cross_ref ON task_tag_cross_ref.taskId = tasks.id " +
            "INNER JOIN tags ON tags.id = task_tag_cross_ref.tagId " +
            "WHERE tags.name = :tagName",
    )
    fun tasksWithTag(tagName: String): Flow<List<Task>>

    @Insert
    suspend fun insertCrossRef(ref: TaskTagCrossRef)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun clearTagsForTask(taskId: Long)
}
