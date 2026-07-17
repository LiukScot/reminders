package com.liukscot.reminders.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemindersDatabaseTest {
    private lateinit var db: RemindersDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadTask_roundTrips() = runTest {
        val listId = db.taskListDao().insert(TaskList(name = "Groceries"))
        db.taskDao().insert(Task(listId = listId, title = "Buy milk", createdAt = 0))

        val tasks = db.taskDao().getByList(listId).first()

        assertEquals(1, tasks.size)
        assertEquals("Buy milk", tasks.first().title)
    }

    @Test
    fun deletingList_cascadesToItsTasks() = runTest {
        val listId = db.taskListDao().insert(TaskList(name = "Groceries"))
        db.taskDao().insert(Task(listId = listId, title = "Buy milk", createdAt = 0))
        val list = db.taskListDao().getById(listId)!!

        db.taskListDao().delete(list)

        val tasks = db.taskDao().getByList(listId).first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun setTags_dedupesAndFindsTasksAcrossLists() = runTest {
        val repository = RemindersRepository(db)
        val groceriesId = db.taskListDao().insert(TaskList(name = "Groceries"))
        val personalId = db.taskListDao().insert(TaskList(name = "Personal"))
        val milkId = repository.addTask(Task(listId = groceriesId, title = "Buy milk", createdAt = 0))
        val giftId = repository.addTask(Task(listId = personalId, title = "Buy gift", createdAt = 0))

        repository.setTags(milkId, listOf("errands", " errands ", "urgent", ""))
        repository.setTags(giftId, listOf("urgent"))

        val tagsByTaskId = repository.tagsByTaskId.first()
        assertEquals(listOf("errands", "urgent"), tagsByTaskId[milkId])
        assertEquals(listOf("urgent"), tagsByTaskId[giftId])

        val urgentTasks = repository.tasksByTag("urgent").first()
        assertEquals(setOf(milkId, giftId), urgentTasks.map { it.id }.toSet())
    }

    @Test
    fun searchTasks_matchesTitleNotesAndTags_acrossListsIncludingCompleted() = runTest {
        val repository = RemindersRepository(db)
        val groceriesId = db.taskListDao().insert(TaskList(name = "Groceries"))
        val personalId = db.taskListDao().insert(TaskList(name = "Personal"))
        val byTitleId = repository.addTask(Task(listId = groceriesId, title = "Pick up birthday cake", createdAt = 0))
        val byNotesId = repository.addTask(
            Task(listId = personalId, title = "Call the bakery", notes = "about the birthday order", createdAt = 0),
        )
        val byTagId = repository.addTask(
            Task(listId = personalId, title = "Wrap the gift", completed = true, createdAt = 0),
        )
        repository.setTags(byTagId, listOf("birthday"))
        repository.addTask(Task(listId = groceriesId, title = "Buy milk", createdAt = 0))

        val hits = repository.searchTasks("birthday").first()

        assertEquals(setOf(byTitleId, byNotesId, byTagId), hits.map { it.id }.toSet())
    }

    @Test
    fun searchTasks_treatsWildcardsAsLiteralText() = runTest {
        val repository = RemindersRepository(db)
        val listId = db.taskListDao().insert(TaskList(name = "Personal"))
        val discountId = repository.addTask(Task(listId = listId, title = "50% off coupon", createdAt = 0))
        repository.addTask(Task(listId = listId, title = "Buy milk", createdAt = 0))

        // Unescaped, "%" would be a LIKE wildcard and match every task.
        val hits = repository.searchTasks("50%").first()

        assertEquals(listOf(discountId), hits.map { it.id })
    }

    @Test
    fun tasksDueBetween_returnsOnlyTasksForTheSelectedDay() = runTest {
        val listId = db.taskListDao().insert(TaskList(name = "Personal"))
        val startOfDay = 1_752_364_800_000L
        val endOfDay = startOfDay + 86_400_000L
        val firstTaskId = db.taskDao().insert(
            Task(listId = listId, title = "Morning task", dueAt = startOfDay, createdAt = 0),
        )
        val secondTaskId = db.taskDao().insert(
            Task(listId = listId, title = "Evening task", dueAt = endOfDay - 1, createdAt = 0),
        )
        db.taskDao().insert(Task(listId = listId, title = "Tomorrow", dueAt = endOfDay, createdAt = 0))

        val tasks = db.taskDao().getDueBetween(startOfDay, endOfDay).first()

        assertEquals(listOf(firstTaskId, secondTaskId), tasks.map { it.id })
    }

    @Test
    fun openTasksDueBefore_excludesCompletedAndSelectedDayTasks() = runTest {
        val listId = db.taskListDao().insert(TaskList(name = "Personal"))
        val startOfDay = 1_752_364_800_000L
        val overdueId = db.taskDao().insert(
            Task(listId = listId, title = "Overdue", dueAt = startOfDay - 1, createdAt = 0),
        )
        db.taskDao().insert(
            Task(listId = listId, title = "Completed", dueAt = startOfDay - 2, completed = true, createdAt = 0),
        )
        db.taskDao().insert(
            Task(listId = listId, title = "Today", dueAt = startOfDay, createdAt = 0),
        )

        val tasks = db.taskDao().getOpenDueBefore(startOfDay).first()

        assertEquals(listOf(overdueId), tasks.map { it.id })
    }
}
