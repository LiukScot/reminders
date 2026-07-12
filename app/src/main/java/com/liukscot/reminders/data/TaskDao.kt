package com.liukscot.reminders.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SmartCounts(
    val flagged: Int,
    val noDate: Int,
    val all: Int,
    val completed: Int,
)

data class ListCount(val listId: Long, val count: Int)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY createdAt DESC")
    fun getByList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query(
        "SELECT " +
            "SUM(CASE WHEN flagged = 1 AND completed = 0 THEN 1 ELSE 0 END) AS flagged, " +
            "SUM(CASE WHEN dueAt IS NULL AND completed = 0 THEN 1 ELSE 0 END) AS noDate, " +
            "SUM(CASE WHEN completed = 0 THEN 1 ELSE 0 END) AS `all`, " +
            "SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) AS completed " +
            "FROM tasks",
    )
    fun smartCounts(): Flow<SmartCounts>

    @Query("SELECT listId, COUNT(*) AS count FROM tasks WHERE completed = 0 GROUP BY listId")
    fun openCountsByList(): Flow<List<ListCount>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
