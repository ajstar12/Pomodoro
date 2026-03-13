package com.arjunsinh.pomodoro.data.repository

import com.arjunsinh.pomodoro.data.dao.SessionDao
import com.arjunsinh.pomodoro.data.entity.Session
import com.arjunsinh.pomodoro.utils.Constants
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {

    val allSessions: Flow<List<Session>> = sessionDao.getAllSessions()

    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<Session>> {
        return sessionDao.getSessionsInRange(startTime, endTime)
    }

    fun getSessionsForTask(taskId: Long): Flow<List<Session>> {
        return sessionDao.getSessionsForTask(taskId)
    }

    suspend fun getSessionCount(sessionType: Constants.SessionType, startTime: Long): Int {
        return sessionDao.getSessionCount(sessionType, startTime)
    }

    suspend fun getTotalFocusTime(startTime: Long, endTime: Long): Long {
        return sessionDao.getTotalFocusTime(startTime, endTime) ?: 0L
    }

    suspend fun getTotalXpEarned(startTime: Long): Int {
        return sessionDao.getTotalXpEarned(startTime) ?: 0
    }

    suspend fun insertSession(session: Session): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session)
    }

    suspend fun deleteOldSessions(beforeTime: Long) {
        sessionDao.deleteOldSessions(beforeTime)
    }

    fun getRecentFocusSessions(limit: Int): Flow<List<Session>> {
        return sessionDao.getRecentFocusSessions(limit)
    }
}
