package com.liukscot.reminders.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY name ASC")
    fun getAll(): Flow<List<TaskList>>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getById(id: Long): TaskList?

    @Query("SELECT * FROM task_lists")
    suspend fun getAllOnce(): List<TaskList>

    @Insert
    suspend fun insert(taskList: TaskList): Long

    @Insert
    suspend fun insertAll(lists: List<TaskList>)

    // Cascades through tasks and their tag cross-refs, so this alone empties everything but `tags`.
    @Query("DELETE FROM task_lists")
    suspend fun deleteAll()

    @Update
    suspend fun update(taskList: TaskList)

    @Delete
    suspend fun delete(taskList: TaskList)
}
