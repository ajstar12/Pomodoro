package com.arjunsinh.pomodoro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arjunsinh.pomodoro.data.dao.SessionDao
import com.arjunsinh.pomodoro.data.dao.TaskDao
import com.arjunsinh.pomodoro.data.entity.Session
import com.arjunsinh.pomodoro.data.entity.Task
import com.arjunsinh.pomodoro.utils.Constants

@Database(
    entities = [Task::class, Session::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: PomodoroDatabase? = null

        fun getDatabase(context: Context): PomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PomodoroDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
