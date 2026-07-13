package com.liukscot.reminders.data

import kotlinx.coroutines.flow.Flow

class RemindersRepository(
    private val taskDao: TaskDao,
    private val taskListDao: TaskListDao,
) {
    val lists: Flow<List<TaskList>> = taskListDao.getAll()
    val openCountsByList: Flow<List<ListCount>> = taskDao.openCountsByList()

    fun tasksIn(listId: Long): Flow<List<Task>> = taskDao.getByList(listId)

    suspend fun addList(name: String, icon: String): Long =
        taskListDao.insert(TaskList(name = name, icon = icon))

    suspend fun renameList(list: TaskList, newName: String) =
        taskListDao.update(list.copy(name = newName))

    suspend fun deleteList(list: TaskList) = taskListDao.delete(list)

    suspend fun addTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)
}
