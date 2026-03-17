package com.arjunsinh.pomodoro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new started_at column
                db.execSQL("ALTER TABLE sessions ADD COLUMN started_at INTEGER NOT NULL DEFAULT 0")
                // Estimate started_at for old sessions
                db.execSQL("UPDATE sessions SET started_at = completed_at - duration")
            }
        }

        fun getDatabase(context: Context): PomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PomodoroDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
