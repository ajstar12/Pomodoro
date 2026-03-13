package com.arjunsinh.pomodoro.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arjunsinh.pomodoro.R
import com.arjunsinh.pomodoro.service.PomodoroForegroundService
import com.arjunsinh.pomodoro.ui.activities.HomeActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Constants.NOTIFICATION_CHANNEL_DESC
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildTimerNotification(
        remainingTime: String,
        sessionType: Constants.SessionType,
        isRunning: Boolean
    ): Notification {
        val sessionName = when (sessionType) {
            Constants.SessionType.FOCUS -> "Focus Session"
            Constants.SessionType.SHORT_BREAK -> "Short Break"
            Constants.SessionType.LONG_BREAK -> "Long Break"
        }

        val contentIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(sessionName)
            .setContentText("Time remaining: $remainingTime")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Add action buttons
        if (isRunning) {
            builder.addAction(createAction(Constants.ACTION_PAUSE, "Pause", R.drawable.ic_pause))
        } else {
            builder.addAction(createAction(Constants.ACTION_RESUME, "Resume", R.drawable.ic_play))
        }

        builder.addAction(createAction(Constants.ACTION_SKIP, "Skip", R.drawable.ic_skip))
        builder.addAction(createAction(Constants.ACTION_STOP, "Stop", R.drawable.ic_stop))

        return builder.build()
    }

    private fun createAction(action: String, title: String, icon: Int): NotificationCompat.Action {
        val intent = Intent(context, PomodoroForegroundService::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    fun showSessionCompleteNotification(sessionType: Constants.SessionType) {
        val title = when (sessionType) {
            Constants.SessionType.FOCUS -> "Focus Session Complete! 🎉"
            Constants.SessionType.SHORT_BREAK -> "Break Complete!"
            Constants.SessionType.LONG_BREAK -> "Long Break Complete!"
        }

        val message = when (sessionType) {
            Constants.SessionType.FOCUS -> "Great work! Time for a break."
            Constants.SessionType.SHORT_BREAK -> "Ready to focus again?"
            Constants.SessionType.LONG_BREAK -> "Feeling refreshed? Let's continue!"
        }

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_check)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun createDailyReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "daily_reminder_channel",
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily focus reminders"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
