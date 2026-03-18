package com.arjunsinh.pomodoro.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.widget.Toast
import com.arjunsinh.pomodoro.data.database.PomodoroDatabase
import com.arjunsinh.pomodoro.data.entity.Session
import com.arjunsinh.pomodoro.data.repository.SessionRepository
import com.arjunsinh.pomodoro.data.repository.TaskRepository
import com.arjunsinh.pomodoro.utils.Constants
import com.arjunsinh.pomodoro.utils.NotificationHelper
import com.arjunsinh.pomodoro.utils.TimerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class PomodoroForegroundService : Service() {

    private val binder = LocalBinder()
    private lateinit var timerManager: TimerManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var sessionRepository: SessionRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _timerState = MutableStateFlow(TimerStateData())
    val timerState: StateFlow<TimerStateData> = _timerState

    private var currentTaskId: Long? = null

    inner class LocalBinder : Binder() {
        fun getService(): PomodoroForegroundService = this@PomodoroForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        timerManager = TimerManager()
        notificationHelper = NotificationHelper(this)

        val database = PomodoroDatabase.getDatabase(this)
        sessionRepository = SessionRepository(database.sessionDao())
        taskRepository = TaskRepository(database.taskDao())

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START -> handleStart(intent)
            Constants.ACTION_PAUSE -> handlePause()
            Constants.ACTION_RESUME -> handleResume()
            Constants.ACTION_SKIP -> handleSkip()
            Constants.ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun handleStart(intent: Intent) {
        val sessionType = intent.getSerializableExtra(Constants.EXTRA_SESSION_TYPE) as? Constants.SessionType
            ?: Constants.SessionType.FOCUS
        
        serviceScope.launch {
            if (sessionType == Constants.SessionType.FOCUS) {
                val startOfDay = getStartOfDay()
                val todayCount = sessionRepository.getSessionCount(Constants.SessionType.FOCUS, startOfDay)
                
                if (todayCount >= 12) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PomodoroForegroundService, "Daily limit of 12 sessions reached!", Toast.LENGTH_LONG).show()
                    }
                    stopSelf()
                    return@launch
                }
            }

            val duration = getDurationForSessionType(sessionType)
            currentTaskId = intent.getLongExtra("task_id", -1L).takeIf { it != -1L }

            timerManager.startSession(duration, sessionType)
            updateTimerState() 
            startForeground(Constants.FOREGROUND_SERVICE_ID, buildNotification())
            startTimerTicker()
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun handlePause() {
        timerManager.pauseSession()
        stopTimerTicker()
        updateTimerState() 
        updateNotification()
    }

    private fun handleResume() {
        timerManager.resumeSession()
        updateTimerState() 
        updateNotification()
        startTimerTicker()
    }

    private fun handleSkip() {
        saveCurrentSession(false)
        transitionToNextSession()
    }

    private fun handleStop() {
        timerManager.stopSession()
        stopTimerTicker()
        updateTimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimerTicker() {
        stopTimerTicker()
        timerJob = serviceScope.launch {
            while (isActive && timerManager.currentState == Constants.TimerState.RUNNING) {
                if (timerManager.isSessionComplete()) {
                    onSessionComplete()
                    break
                }

                updateTimerState()
                updateNotification()

                delay(1000)
            }
        }
    }

    private fun stopTimerTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun onSessionComplete() {
        timerManager.completeSession()

        // Save session and update tasks
        saveCurrentSession(true)

        // Vibrate
        if (sharedPreferences.getBoolean(Constants.PREFS_ENABLE_VIBRATION, true)) {
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
        }

        // Show completion notification
        notificationHelper.showSessionCompleteNotification(timerManager.currentSessionType)

        // Update gamification
        if (timerManager.currentSessionType == Constants.SessionType.FOCUS) {
            updateGamificationStats()
        }

        // Broadcast completion
        sendBroadcast(Intent(Constants.BROADCAST_SESSION_COMPLETE).apply {
            putExtra(Constants.EXTRA_SESSION_TYPE, timerManager.currentSessionType)
        })

        // Auto-start next session if enabled
        if (sharedPreferences.getBoolean(Constants.PREFS_AUTO_START_NEXT, false)) {
            serviceScope.launch {
                delay(3000)
                transitionToNextSession()
            }
        } else {
            updateTimerState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun transitionToNextSession() {
        val nextSessionType = timerManager.getNextSessionType()
        
        serviceScope.launch {
            if (nextSessionType == Constants.SessionType.FOCUS) {
                val startOfDay = getStartOfDay()
                val todayCount = sessionRepository.getSessionCount(Constants.SessionType.FOCUS, startOfDay)
                
                if (todayCount >= 12) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PomodoroForegroundService, "Daily limit of 12 sessions reached!", Toast.LENGTH_LONG).show()
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
            }

            val duration = getDurationForSessionType(nextSessionType)
            timerManager.startSession(duration, nextSessionType)
            updateTimerState()
            startForeground(Constants.FOREGROUND_SERVICE_ID, buildNotification())
            startTimerTicker()
        }
    }

    private fun saveCurrentSession(wasCompleted: Boolean) {
        val sessionType = timerManager.currentSessionType
        val duration = timerManager.getSessionDuration()
        val startTime = timerManager.getSessionStartTime()

        val xpEarned = if (wasCompleted && sessionType == Constants.SessionType.FOCUS) {
            Constants.XP_PER_FOCUS_SESSION
        } else 0

        val session = Session(
            taskId = currentTaskId,
            duration = duration,
            sessionType = sessionType,
            startedAt = startTime,
            completedAt = System.currentTimeMillis(),
            xpEarned = xpEarned,
            wasCompleted = wasCompleted
        )

        serviceScope.launch {
            sessionRepository.insertSession(session)

            // When a FOCUS session is completed, increment all active tasks
            if (wasCompleted && sessionType == Constants.SessionType.FOCUS) {
                taskRepository.incrementAllActiveTasksPomodoros()
            }
        }
    }

    private fun updateGamificationStats() {
        val currentXP = sharedPreferences.getInt(Constants.PREFS_TOTAL_XP, 0)
        val newXP = currentXP + Constants.XP_PER_FOCUS_SESSION
        val currentLevel = sharedPreferences.getInt(Constants.PREFS_USER_LEVEL, 1)
        val newLevel = (newXP / Constants.XP_PER_LEVEL) + 1

        sharedPreferences.edit().apply {
            putInt(Constants.PREFS_TOTAL_XP, newXP)
            putInt(Constants.PREFS_USER_LEVEL, newLevel)
            apply()
        }
    }

    private fun updateTimerState() {
        serviceScope.launch {
            val startOfDay = getStartOfDay()
            val todayCount = sessionRepository.getSessionCount(Constants.SessionType.FOCUS, startOfDay)
            
            _timerState.value = TimerStateData(
                remainingTime = timerManager.getRemainingTime(),
                sessionType = timerManager.currentSessionType,
                timerState = timerManager.currentState,
                sessionCount = todayCount, // Using daily count here
                progress = timerManager.getProgress()
            )

            sendBroadcast(Intent(Constants.BROADCAST_TIMER_UPDATE).apply {
                putExtra(Constants.EXTRA_REMAINING_TIME, timerManager.getRemainingTime())
                putExtra(Constants.EXTRA_SESSION_TYPE, timerManager.currentSessionType)
                putExtra(Constants.EXTRA_IS_RUNNING, timerManager.currentState == Constants.TimerState.RUNNING)
                putExtra(Constants.EXTRA_CURRENT_SESSION_COUNT, todayCount) // Sending daily count
            })
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        notificationHelper.updateNotification(Constants.FOREGROUND_SERVICE_ID, notification)
    }

    private fun buildNotification() = notificationHelper.buildTimerNotification(
        remainingTime = timerManager.getFormattedTime(),
        sessionType = timerManager.currentSessionType,
        isRunning = timerManager.currentState == Constants.TimerState.RUNNING
    )

    private fun getDurationForSessionType(sessionType: Constants.SessionType): Long {
        return when (sessionType) {
            Constants.SessionType.FOCUS -> 
                sharedPreferences.getLong(Constants.PREFS_FOCUS_DURATION, Constants.DEFAULT_FOCUS_DURATION)
            Constants.SessionType.SHORT_BREAK -> 
                sharedPreferences.getLong(Constants.PREFS_SHORT_BREAK_DURATION, Constants.DEFAULT_SHORT_BREAK_DURATION)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PomodoroApp::TimerWakeLock"
        )
        wakeLock?.acquire(60 * 60 * 1000L) 
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerTicker()
        serviceScope.cancel()
        wakeLock?.release()
    }

    data class TimerStateData(
        val remainingTime: Long = 0L,
        val sessionType: Constants.SessionType = Constants.SessionType.FOCUS,
        val timerState: Constants.TimerState = Constants.TimerState.IDLE,
        val sessionCount: Int = 0,
        val progress: Float = 0f
    )

    fun getTimerManager() = timerManager
    fun setCurrentTask(taskId: Long?) {
        currentTaskId = taskId
    }
}
