package com.arjunsinh.pomodoro.utils

object Constants {
    // Timer Defaults (in milliseconds)
    const val DEFAULT_FOCUS_DURATION = 10 * 1000L // 25 minutes
    const val DEFAULT_SHORT_BREAK_DURATION = 10 * 1000L // 5 minutes

    // Service
    const val FOREGROUND_SERVICE_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "pomodoro_timer_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Pomodoro Timer"
    const val NOTIFICATION_CHANNEL_DESC = "Shows timer progress"
    const val NOTIFICATION_COMPLETION_CHANNEL_ID = "pomodoro_completion_channel"
    const val NOTIFICATION_COMPLETION_CHANNEL_NAME = "Session Completion"
    const val NOTIFICATION_COMPLETION_CHANNEL_DESC = "Alerts when sessions complete"

    // Notification Actions
    const val ACTION_START = "ACTION_START"
    const val ACTION_PAUSE = "ACTION_PAUSE"
    const val ACTION_RESUME = "ACTION_RESUME"
    const val ACTION_SKIP = "ACTION_SKIP"
    const val ACTION_STOP = "ACTION_STOP"

    // Broadcast Actions
    const val BROADCAST_TIMER_UPDATE = "com.arjunsinh.pomodoro.TIMER_UPDATE"
    const val BROADCAST_SESSION_COMPLETE = "com.arjunsinh.pomodoro.SESSION_COMPLETE"

    // Intent Extras
    const val EXTRA_REMAINING_TIME = "remaining_time"
    const val EXTRA_SESSION_TYPE = "session_type"
    const val EXTRA_IS_RUNNING = "is_running"
    const val EXTRA_CURRENT_SESSION_COUNT = "current_session_count"

    // Session Types
    enum class SessionType {
        FOCUS, SHORT_BREAK
    }

    // Timer States
    enum class TimerState {
        IDLE, RUNNING, PAUSED, COMPLETED
    }

    // SharedPreferences Keys
    const val PREFS_NAME = "pomodoro_prefs"
    const val PREFS_FOCUS_DURATION = "focus_duration"
    const val PREFS_SHORT_BREAK_DURATION = "short_break_duration"
    const val PREFS_ENABLE_VIBRATION = "enable_vibration"
    const val PREFS_ENABLE_FOCUS_SOUND = "enable_focus_sound"
    const val PREFS_ENABLE_BREAK_SOUND = "enable_break_sound"
    const val PREFS_NOTIFICATION_SOUND_URI = "notification_sound_uri"
    const val PREFS_ENABLE_SOUND = "enable_sound"
    const val PREFS_AUTO_START_NEXT = "auto_start_next"
    const val PREFS_DAILY_GOAL = "daily_goal"
    const val PREFS_TOTAL_XP = "total_xp"
    const val PREFS_USER_LEVEL = "user_level"
    const val PREFS_FOCUS_STREAK = "focus_streak"
    const val PREFS_LAST_FOCUS_DATE = "last_focus_date"

    // Gamification
    const val XP_PER_FOCUS_SESSION = 100
    const val XP_PER_LEVEL = 500
    const val STREAK_BONUS_XP = 50

    // Database
    const val DATABASE_NAME = "pomodoro_database"
    const val DATABASE_VERSION = 2

    // Notification IDs
    const val REMINDER_NOTIFICATION_ID = 2001

    // Work Manager
    const val DAILY_REMINDER_WORK = "daily_reminder_work"
}
