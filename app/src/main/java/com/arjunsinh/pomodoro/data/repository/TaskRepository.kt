package com.arjunsinh.pomodoro.data.repository

import com.arjunsinh.pomodoro.data.dao.TaskDao
import com.arjunsinh.pomodoro.data.entity.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allActiveTasks: Flow<List<Task>> = taskDao.getAllActiveTasks()
    val allCompletedTasks: Flow<List<Task>> = taskDao.getAllCompletedTasks()
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun incrementTaskPomodoros(taskId: Long) {
        taskDao.incrementTaskPomodoros(taskId)
    }

    suspend fun incrementAllActiveTasksPomodoros() {
        taskDao.incrementAllActiveTasksPomodoros()
    }

    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted, System.currentTimeMillis())
    }

    suspend fun deleteAllCompletedTasks() {
        taskDao.deleteAllCompletedTasks()
    }
}
