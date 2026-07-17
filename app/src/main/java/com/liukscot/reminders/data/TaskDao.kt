package com.liukscot.reminders.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ListCount(val listId: Long, val count: Int)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY priority DESC, createdAt DESC")
    fun getByList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueAt >= :startInclusive AND dueAt < :endExclusive ORDER BY dueAt ASC, priority DESC, createdAt DESC")
    fun getDueBetween(startInclusive: Long, endExclusive: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueAt < :beforeExclusive AND completed = 0 ORDER BY dueAt ASC, priority DESC, createdAt DESC")
    fun getOpenDueBefore(beforeExclusive: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    // Plain LIKE, not FTS: the table is one person's reminders, so a scan is nowhere near a
    // measured bottleneck. `pattern` arrives already wrapped in %…% and escaped by the caller.
    @Query(
        """
        SELECT DISTINCT tasks.* FROM tasks
        LEFT JOIN task_tag_cross_ref ON task_tag_cross_ref.taskId = tasks.id
        LEFT JOIN tags ON tags.id = task_tag_cross_ref.tagId
        WHERE tasks.title LIKE :pattern ESCAPE '\'
           OR tasks.notes LIKE :pattern ESCAPE '\'
           OR tags.name LIKE :pattern ESCAPE '\'
        ORDER BY tasks.dueAt IS NULL, tasks.dueAt ASC, tasks.createdAt DESC
        """,
    )
    fun search(pattern: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE hasDueTime = 1 AND completed = 0 AND dueAt >= :fromInclusive")
    suspend fun getPendingRemindersFrom(fromInclusive: Long): List<Task>

    @Query("SELECT listId, COUNT(*) AS count FROM tasks WHERE completed = 0 GROUP BY listId")
    fun openCountsByList(): Flow<List<ListCount>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllOnce(): List<Task>

    @Insert
    suspend fun insertAll(tasks: List<Task>)

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
