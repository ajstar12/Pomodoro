package com.arjunsinh.pomodoro.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.arjunsinh.pomodoro.utils.Constants

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long? = null,

    @ColumnInfo(name = "duration")
    val duration: Long, // in milliseconds

    @ColumnInfo(name = "session_type")
    val sessionType: Constants.SessionType,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "xp_earned")
    val xpEarned: Int = 0,

    @ColumnInfo(name = "was_completed")
    val wasCompleted: Boolean = true // false if skipped
)
