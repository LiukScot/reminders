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

    @Query("SELECT COUNT(*) FROM task_lists")
    suspend fun count(): Int

    @Insert
    suspend fun insert(taskList: TaskList): Long

    @Update
    suspend fun update(taskList: TaskList)

    @Delete
    suspend fun delete(taskList: TaskList)
}
