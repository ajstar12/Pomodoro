package com.arjunsinh.pomodoro.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arjunsinh.pomodoro.data.database.PomodoroDatabase
import com.arjunsinh.pomodoro.data.repository.SessionRepository
import com.arjunsinh.pomodoro.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepository: SessionRepository
    private val sharedPreferences: SharedPreferences

    private val _stats = MutableStateFlow(Statistics())
    val stats: StateFlow<Statistics> = _stats

    init {
        val database = PomodoroDatabase.getDatabase(application)
        sessionRepository = SessionRepository(database.sessionDao())
        sharedPreferences = application.getSharedPreferences(Constants.PREFS_NAME, Application.MODE_PRIVATE)

        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            val todayStart = getTodayStart()
            val todayEnd = getTodayEnd()
            val weekStart = getWeekStart()

            val todayFocusTime = sessionRepository.getTotalFocusTime(todayStart, todayEnd)
            val weekFocusTime = sessionRepository.getTotalFocusTime(weekStart, todayEnd)
            val todaySessionCount = sessionRepository.getSessionCount(Constants.SessionType.FOCUS, todayStart)
            val totalXP = sharedPreferences.getInt(Constants.PREFS_TOTAL_XP, 0)
            val userLevel = sharedPreferences.getInt(Constants.PREFS_USER_LEVEL, 1)
            val focusStreak = sharedPreferences.getInt(Constants.PREFS_FOCUS_STREAK, 0)

            _stats.value = Statistics(
                todayFocusTime = todayFocusTime,
                weekFocusTime = weekFocusTime,
                todaySessionCount = todaySessionCount,
                totalXP = totalXP,
                userLevel = userLevel,
                focusStreak = focusStreak
            )
        }
    }

    private fun getTodayStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getTodayEnd(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun getWeekStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    data class Statistics(
        val todayFocusTime: Long = 0L,
        val weekFocusTime: Long = 0L,
        val todaySessionCount: Int = 0,
        val totalXP: Int = 0,
        val userLevel: Int = 1,
        val focusStreak: Int = 0
    )
}
