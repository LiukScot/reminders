package com.liukscot.reminders.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, TaskList::class, Tag::class, TaskTagCrossRef::class], version = 8)
abstract class RemindersDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var instance: RemindersDatabase? = null

        fun getInstance(context: Context): RemindersDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RemindersDatabase::class.java,
                    "reminders.db",
                )
                    // ponytail: pre-release, no shipped users yet — real migrations
                    // start once the app has installs to preserve.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : RoomDatabase.Callback() {
                        // Seeding on open rather than onCreate: a destructive migration recreates
                        // the tables empty while the db file lives on, so onCreate never fires and
                        // the app would come back with no lists and nowhere to put a reminder.
                        // onDestructiveMigration is no good either — it runs after the drop but
                        // before the tables are recreated, so the insert has nothing to write to.
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedDefaultList(db)
                        }
                    })
                    .build().also { instance = it }
            }

        // Conditional, because this runs on every open: it fills an empty table and is a no-op
        // otherwise. Deleting the last list is not allowed anywhere in the app, so "no lists at
        // all" only ever means a freshly created or freshly wiped schema.
        private fun seedDefaultList(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO task_lists (name, icon) " +
                    "SELECT 'Personal', 'inbox' WHERE NOT EXISTS (SELECT 1 FROM task_lists)",
            )
        }
    }
}
