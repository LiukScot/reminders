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

    fun tasksDueBetween(startInclusive: Long, endExclusive: Long): Flow<List<Task>> =
        taskDao.getDueBetween(startInclusive, endExclusive)

    fun openTasksDueBefore(beforeExclusive: Long): Flow<List<Task>> =
        taskDao.getOpenDueBefore(beforeExclusive)

    fun tasksByTag(tagName: String): Flow<List<Task>> = tagDao.tasksByTagName(tagName)

    suspend fun pendingRemindersFrom(fromInclusive: Long): List<Task> =
        taskDao.getPendingRemindersFrom(fromInclusive)

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

    // Shared by every "New/Edit reminder" call site (Day, Week, Lists, list detail) — was
    // duplicated four times before, each building the same Task and re-persisting tags.
    suspend fun saveTask(
        existing: Task?,
        title: String,
        notes: String?,
        listId: Long,
        tags: List<String>,
        flagged: Boolean,
        priority: Int,
        dueAt: Long?,
        hasDueTime: Boolean,
        recurrenceFreq: String?,
        recurrenceInterval: Int,
        recurrenceByDay: String?,
        recurrenceAnchor: Long?,
    ): Task {
        val saved = if (existing != null) {
            existing.copy(
                title = title,
                notes = notes,
                listId = listId,
                flagged = flagged,
                priority = priority,
                dueAt = dueAt,
                hasDueTime = hasDueTime,
                recurrenceFreq = recurrenceFreq,
                recurrenceInterval = recurrenceInterval,
                recurrenceByDay = recurrenceByDay,
                recurrenceAnchor = recurrenceAnchor,
            ).also { taskDao.update(it) }
        } else {
            val newTask = Task(
                listId = listId,
                title = title,
                notes = notes,
                flagged = flagged,
                priority = priority,
                dueAt = dueAt,
                hasDueTime = hasDueTime,
                createdAt = System.currentTimeMillis(),
                recurrenceFreq = recurrenceFreq,
                recurrenceInterval = recurrenceInterval,
                recurrenceByDay = recurrenceByDay,
                recurrenceAnchor = recurrenceAnchor,
            )
            newTask.copy(id = taskDao.insert(newTask))
        }
        setTags(saved.id, tags)
        return saved
    }

    // A recurring task never stays completed: finishing an occurrence advances it to the next
    // one per its rule (anchor included, so the cadence keeps ticking on schedule) and reopens
    // it, instead of marking the row done. Un-completing follows the plain path unconditionally.
    suspend fun toggleComplete(task: Task): Task {
        val rule = RecurrenceRule.fromTask(task)
        val updated = if (!task.completed && rule != null && task.recurrenceAnchor != null) {
            val next = nextOccurrence(task.recurrenceAnchor, rule)
            task.copy(dueAt = next, recurrenceAnchor = next, completed = false)
        } else {
            task.copy(completed = !task.completed)
        }
        taskDao.update(updated)
        return updated
    }

    suspend fun deleteTask(task: Task) = taskDao.delete(task)
}
