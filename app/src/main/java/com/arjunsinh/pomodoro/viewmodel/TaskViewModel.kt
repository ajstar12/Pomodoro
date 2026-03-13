package com.arjunsinh.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arjunsinh.pomodoro.data.database.PomodoroDatabase
import com.arjunsinh.pomodoro.data.entity.Task
import com.arjunsinh.pomodoro.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {


    private val repository: TaskRepository

    val allActiveTasks: StateFlow<List<Task>>
    val allCompletedTasks: StateFlow<List<Task>>

    init {
        val taskDao = PomodoroDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)

        allActiveTasks = repository.allActiveTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCompletedTasks = repository.allCompletedTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateTask(updatedTask)
    }

    fun deleteAllCompletedTasks() = viewModelScope.launch {
        repository.deleteAllCompletedTasks()
    }
}
