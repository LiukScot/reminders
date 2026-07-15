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
                        // Fires exactly once, when the db file is first created —
                        // seeds the default "Personal" list for a fresh install.
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            db.execSQL("INSERT INTO task_lists (name, icon) VALUES ('Personal', 'inbox')")
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
