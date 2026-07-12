package com.liukscot.reminders.data

import kotlinx.coroutines.flow.Flow

class RemindersRepository(
    private val taskDao: TaskDao,
    private val taskListDao: TaskListDao,
    private val tagDao: TagDao,
) {
    val lists: Flow<List<TaskList>> = taskListDao.getAll()
    val smartCounts: Flow<SmartCounts> = taskDao.smartCounts()
    val openCountsByList: Flow<List<ListCount>> = taskDao.openCountsByList()

    fun tasksIn(listId: Long): Flow<List<Task>> = taskDao.getByList(listId)

    fun tagNamesForList(listId: Long): Flow<List<TaskTagName>> = tagDao.tagNamesForList(listId)

    fun tasksWithTag(tagName: String): Flow<List<Task>> = tagDao.tasksWithTag(tagName)

    // Mockup edits a task's tags as one space-separated field, so saving
    // always replaces the full set rather than adding/removing one at a time.
    suspend fun setTags(taskId: Long, tagNames: List<String>) {
        tagDao.clearTagsForTask(taskId)
        tagNames.distinct().forEach { name ->
            val tagId = tagDao.findByName(name)?.id ?: tagDao.insert(Tag(name = name))
            tagDao.insertCrossRef(TaskTagCrossRef(taskId = taskId, tagId = tagId))
        }
    }

    suspend fun addList(name: String, icon: String): Long =
        taskListDao.insert(TaskList(name = name, icon = icon))

    suspend fun renameList(list: TaskList, newName: String) =
        taskListDao.update(list.copy(name = newName))

    suspend fun deleteList(list: TaskList) = taskListDao.delete(list)

    suspend fun addTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun isEmpty(): Boolean = taskListDao.getById(1) == null && taskDao.getById(1) == null
}
