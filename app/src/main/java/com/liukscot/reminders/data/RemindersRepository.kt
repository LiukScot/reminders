package com.liukscot.reminders.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemindersRepository(
    private val taskDao: TaskDao,
    private val taskListDao: TaskListDao,
    private val tagDao: TagDao,
) {
    val lists: Flow<List<TaskList>> = taskListDao.getAll()
    val openCountsByList: Flow<List<ListCount>> = taskDao.openCountsByList()
    val tagsByTaskId: Flow<Map<Long, List<String>>> =
        tagDao.tagsByTaskId().map { rows -> rows.groupBy(TaskTagRow::taskId, TaskTagRow::name) }

    fun tasksIn(listId: Long): Flow<List<Task>> = taskDao.getByList(listId)

    fun tasksByTag(tagName: String): Flow<List<Task>> = tagDao.tasksByTagName(tagName)

    suspend fun setTags(taskId: Long, tagNames: List<String>) {
        val normalized = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val tagIds = normalized.map { name -> tagDao.findByName(name)?.id ?: tagDao.insert(Tag(name = name)) }
        tagDao.clearTagsForTask(taskId)
        tagDao.insertCrossRefs(tagIds.map { tagId -> TaskTagCrossRef(taskId = taskId, tagId = tagId) })
    }

    suspend fun addList(name: String, icon: String): Long =
        taskListDao.insert(TaskList(name = name, icon = icon))

    suspend fun renameList(list: TaskList, newName: String) =
        taskListDao.update(list.copy(name = newName))

    suspend fun deleteList(list: TaskList) = taskListDao.delete(list)

    suspend fun addTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)
}
