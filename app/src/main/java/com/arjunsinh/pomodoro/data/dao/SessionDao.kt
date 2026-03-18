package com.arjunsinh.pomodoro.data.dao

import androidx.room.*
import com.arjunsinh.pomodoro.data.entity.Session
import com.arjunsinh.pomodoro.utils.Constants
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY completed_at DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE completed_at >= :startTime AND completed_at <= :endTime ORDER BY completed_at DESC")
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE task_id = :taskId ORDER BY completed_at DESC")
    fun getSessionsForTask(taskId: Long): Flow<List<Session>>

    @Query("SELECT COUNT(*) FROM sessions WHERE session_type = :sessionType AND completed_at >= :startTime AND was_completed = 1")
    suspend fun getSessionCount(sessionType: Constants.SessionType, startTime: Long): Int

    @Query("SELECT SUM(duration) FROM sessions WHERE session_type = 'FOCUS' AND completed_at >= :startTime AND completed_at <= :endTime AND was_completed = 1")
    suspend fun getTotalFocusTime(startTime: Long, endTime: Long): Long?

    @Query("SELECT SUM(xp_earned) FROM sessions WHERE completed_at >= :startTime AND was_completed = 1")
    suspend fun getTotalXpEarned(startTime: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE completed_at < :beforeTime")
    suspend fun deleteOldSessions(beforeTime: Long)

    @Query("SELECT * FROM sessions WHERE session_type = 'FOCUS' AND was_completed = 1 ORDER BY completed_at DESC LIMIT :limit")
    fun getRecentFocusSessions(limit: Int): Flow<List<Session>>
}
