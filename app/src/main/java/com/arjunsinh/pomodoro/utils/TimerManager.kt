package com.arjunsinh.pomodoro.utils

import android.os.SystemClock
import com.arjunsinh.pomodoro.utils.Constants.SessionType
import com.arjunsinh.pomodoro.utils.Constants.TimerState

class TimerManager {

    private var sessionEndTime: Long = 0L
    private var sessionDuration: Long = 0L
    private var pausedRemainingTime: Long = 0L
    private var sessionStartTimeMillis: Long = 0L

    var currentState: TimerState = TimerState.IDLE
        private set

    var currentSessionType: SessionType = SessionType.FOCUS
        private set

    var currentSessionCount: Int = 0
        private set

    fun startSession(duration: Long, sessionType: SessionType) {
        sessionDuration = duration
        currentSessionType = sessionType
        sessionEndTime = SystemClock.elapsedRealtime() + duration
        sessionStartTimeMillis = System.currentTimeMillis()
        currentState = TimerState.RUNNING
    }

    fun pauseSession() {
        if (currentState == TimerState.RUNNING) {
            pausedRemainingTime = getRemainingTime()
            currentState = TimerState.PAUSED
        }
    }

    fun resumeSession() {
        if (currentState == TimerState.PAUSED) {
            sessionEndTime = SystemClock.elapsedRealtime() + pausedRemainingTime
            currentState = TimerState.RUNNING
            pausedRemainingTime = 0L
        }
    }

    fun stopSession() {
        currentState = TimerState.IDLE
        sessionEndTime = 0L
        pausedRemainingTime = 0L
    }

    fun completeSession() {
        currentState = TimerState.COMPLETED
        if (currentSessionType == SessionType.FOCUS) {
            currentSessionCount++
        }
    }

    fun resetSessionCount() {
        currentSessionCount = 0
    }

    fun getRemainingTime(): Long {
        return when (currentState) {
            TimerState.RUNNING -> {
                val remaining = sessionEndTime - SystemClock.elapsedRealtime()
                if (remaining <= 0) 0L else remaining
            }
            TimerState.PAUSED -> pausedRemainingTime
            else -> 0L
        }
    }

    fun getProgress(): Float {
        if (sessionDuration == 0L) return 0f
        val remaining = getRemainingTime()
        val elapsed = sessionDuration - remaining
        return (elapsed.toFloat() / sessionDuration.toFloat()).coerceIn(0f, 1f)
    }

    fun isSessionComplete(): Boolean {
        return currentState == TimerState.RUNNING && getRemainingTime() <= 0
    }

    fun getNextSessionType(): SessionType {
        return when (currentSessionType) {
            SessionType.FOCUS -> SessionType.SHORT_BREAK
            SessionType.SHORT_BREAK -> SessionType.FOCUS
        }
    }

    fun shouldResetCount(): Boolean {
        return false
    }

    fun getSessionDuration(): Long = sessionDuration

    fun getSessionStartTime(): Long = sessionStartTimeMillis

    fun getFormattedTime(): String {
        val totalSeconds = (getRemainingTime() / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
