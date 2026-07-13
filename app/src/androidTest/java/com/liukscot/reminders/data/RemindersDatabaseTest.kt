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
        val repository = RemindersRepository(db.taskDao(), db.taskListDao(), db.tagDao())
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
}
