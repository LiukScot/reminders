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

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT listId, COUNT(*) AS count FROM tasks WHERE completed = 0 GROUP BY listId")
    fun openCountsByList(): Flow<List<ListCount>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
