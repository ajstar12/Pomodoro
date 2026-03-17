package com.arjunsinh.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arjunsinh.pomodoro.data.database.PomodoroDatabase
import com.arjunsinh.pomodoro.data.repository.SessionRepository
import com.arjunsinh.pomodoro.ui.model.HistoryItem
import com.arjunsinh.pomodoro.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepository: SessionRepository

    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems

    private val _totalCompletedSessions = MutableStateFlow(0)
    val totalCompletedSessions: StateFlow<Int> = _totalCompletedSessions

    init {
        val database = PomodoroDatabase.getDatabase(application)
        sessionRepository = SessionRepository(database.sessionDao())
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            sessionRepository.allSessions.collect { sessionList ->
                val completedSessions = sessionList.filter { 
                    it.wasCompleted && it.sessionType == Constants.SessionType.FOCUS 
                }
                _totalCompletedSessions.value = completedSessions.size
                _historyItems.value = groupSessionsByDate(sessionList)
            }
        }
    }

    private fun groupSessionsByDate(sessions: List<com.arjunsinh.pomodoro.data.entity.Session>): List<HistoryItem> {
        val items = mutableListOf<HistoryItem>()
        val groupedSessions = sessions.groupBy { getDateKey(it.completedAt) }

        groupedSessions.forEach { (dateKey, sessionList) ->
            val dateLabel = getDateLabel(dateKey)
            items.add(HistoryItem.DateHeader(dateLabel, dateKey))
            sessionList.forEach { session ->
                items.add(HistoryItem.SessionItem(session))
            }
        }

        return items
    }

    private fun getDateKey(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDateLabel(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        yesterday.set(Calendar.HOUR_OF_DAY, 0)
        yesterday.set(Calendar.MINUTE, 0)
        yesterday.set(Calendar.SECOND, 0)
        yesterday.set(Calendar.MILLISECOND, 0)

        return when {
            timestamp >= today.timeInMillis -> "Today"
            timestamp >= yesterday.timeInMillis -> "Yesterday"
            else -> {
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}
