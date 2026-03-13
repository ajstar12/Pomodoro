package com.arjunsinh.pomodoro.data.dao

import androidx.room.*
import com.arjunsinh.pomodoro.data.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY created_at DESC")
    fun getAllActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE is_completed = 1 ORDER BY updated_at DESC")
    fun getAllCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET completed_pomodoros = completed_pomodoros + 1 WHERE id = :taskId")
    suspend fun incrementTaskPomodoros(taskId: Long)

    @Query("UPDATE tasks SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE is_completed = 1")
    suspend fun deleteAllCompletedTasks()
}
