package com.arjunsinh.pomodoro.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "target_pomodoros")
    val targetPomodoros: Int = 1,

    @ColumnInfo(name = "completed_pomodoros")
    val completedPomodoros: Int = 0,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    fun getProgress(): Int {
        return if (targetPomodoros > 0) {
            ((completedPomodoros.toFloat() / targetPomodoros) * 100).toInt()
        } else 0
    }
}
