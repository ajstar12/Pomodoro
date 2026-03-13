package com.arjunsinh.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arjunsinh.pomodoro.data.database.PomodoroDatabase
import com.arjunsinh.pomodoro.data.repository.SessionRepository
import com.arjunsinh.pomodoro.data.repository.TaskRepository
import com.arjunsinh.pomodoro.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepository: SessionRepository
    private val taskRepository: TaskRepository

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState

    private val _selectedTask = MutableStateFlow<Long?>(null)
    val selectedTask: StateFlow<Long?> = _selectedTask

    init {
        val database = PomodoroDatabase.getDatabase(application)
        sessionRepository = SessionRepository(database.sessionDao())
        taskRepository = TaskRepository(database.taskDao())
    }

    fun updateTimerState(
        remainingTime: Long,
        sessionType: Constants.SessionType,
        isRunning: Boolean,
        sessionCount: Int,
        progress: Float
    ) {
        _timerState.value = TimerState(
            remainingTime = remainingTime,
            sessionType = sessionType,
            isRunning = isRunning,
            sessionCount = sessionCount,
            progress = progress
        )
    }

    fun selectTask(taskId: Long?) {
        _selectedTask.value = taskId
    }

    fun getFormattedTime(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    data class TimerState(
        val remainingTime: Long = 0L,
        val sessionType: Constants.SessionType = Constants.SessionType.FOCUS,
        val isRunning: Boolean = false,
        val sessionCount: Int = 0,
        val progress: Float = 0f
    )
}
