package com.liukscot.reminders.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Soonest first, undated last, then the loudest of what's left — shared by every smart list so
// they read the same way.
private const val SMART_LIST_ORDER = "dueAt IS NULL, dueAt ASC, priority DESC, createdAt DESC"

data class ListCount(val listId: Long, val count: Int)

data class SmartListCounts(
    val flagged: Int = 0,
    val open: Int = 0,
    val completed: Int = 0,
    val overdue: Int = 0,
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY priority DESC, createdAt DESC")
    fun getByList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueAt >= :startInclusive AND dueAt < :endExclusive ORDER BY dueAt ASC, priority DESC, createdAt DESC")
    fun getDueBetween(startInclusive: Long, endExclusive: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueAt < :beforeExclusive AND completed = 0 ORDER BY dueAt ASC, priority DESC, createdAt DESC")
    fun getOpenDueBefore(beforeExclusive: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE flagged = 1 AND completed = 0 ORDER BY $SMART_LIST_ORDER")
    fun getFlagged(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY $SMART_LIST_ORDER")
    fun getAllOpen(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY $SMART_LIST_ORDER")
    fun getCompleted(): Flow<List<Task>>

    // One row of four counters rather than four queries: the home grid always shows all of them,
    // and SUM(condition) counts the rows matching it — SQLite scores a true condition as 1.
    // COALESCE covers the empty table, where SUM is NULL rather than 0.
    @Query(
        """
        SELECT
            COALESCE(SUM(flagged = 1 AND completed = 0), 0) AS flagged,
            COALESCE(SUM(completed = 0), 0) AS open,
            COALESCE(SUM(completed = 1), 0) AS completed,
            COALESCE(SUM(completed = 0 AND dueAt IS NOT NULL AND dueAt < :now), 0) AS overdue
        FROM tasks
        """,
    )
    fun smartListCounts(now: Long): Flow<SmartListCounts>

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
