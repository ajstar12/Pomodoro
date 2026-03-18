package com.arjunsinh.pomodoro.ui.model

import com.arjunsinh.pomodoro.data.entity.Session

sealed class HistoryItem {
    data class StatisticsHeader(val totalSessions: Int) : HistoryItem()
    data class DateHeader(val date: String, val timestamp: Long) : HistoryItem()
    data class SessionItem(val session: Session) : HistoryItem()
}
