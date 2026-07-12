package com.liukscot.reminders.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    fun insertAndReadTask_roundTrips() = runBlocking {
        val listId = db.taskListDao().insert(TaskList(name = "Groceries"))
        db.taskDao().insert(Task(listId = listId, title = "Buy milk", createdAt = 0))

        val tasks = db.taskDao().getByList(listId).first()

        assert(tasks.size == 1)
        assert(tasks.first().title == "Buy milk")
    }

    @Test
    fun deletingList_cascadesToItsTasks() = runBlocking {
        val listId = db.taskListDao().insert(TaskList(name = "Groceries"))
        db.taskDao().insert(Task(listId = listId, title = "Buy milk", createdAt = 0))
        val list = db.taskListDao().getById(listId)!!

        db.taskListDao().delete(list)

        val tasks = db.taskDao().getByList(listId).first()
        assert(tasks.isEmpty())
    }
}
