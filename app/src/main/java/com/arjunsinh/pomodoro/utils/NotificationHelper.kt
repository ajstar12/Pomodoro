package com.arjunsinh.pomodoro.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
            // Timer progress channel (low importance, no sound)
            val timerChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Constants.NOTIFICATION_CHANNEL_DESC
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(timerChannel)

            // Session completion channel (high importance, with sound)
            val completionChannel = NotificationChannel(
                Constants.NOTIFICATION_COMPLETION_CHANNEL_ID,
                Constants.NOTIFICATION_COMPLETION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Constants.NOTIFICATION_COMPLETION_CHANNEL_DESC
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(completionChannel)
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
        val sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

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

        // Determine if sound should play based on settings
        val shouldPlaySound = when (sessionType) {
            Constants.SessionType.FOCUS -> 
                sharedPreferences.getBoolean(Constants.PREFS_ENABLE_FOCUS_SOUND, true)
            Constants.SessionType.SHORT_BREAK, Constants.SessionType.LONG_BREAK -> 
                sharedPreferences.getBoolean(Constants.PREFS_ENABLE_BREAK_SOUND, true)
        }

        val contentIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_COMPLETION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_check)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(null) // Don't use notification's built-in sound

        // Vibration is controlled by the notification channel settings
        val enableVibration = sharedPreferences.getBoolean(Constants.PREFS_ENABLE_VIBRATION, true)
        if (!enableVibration) {
            builder.setVibrate(longArrayOf(0))
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())

        // Play sound manually for 1 second if enabled
        if (shouldPlaySound) {
            playNotificationSound()
        }
    }

    private fun playNotificationSound() {
        try {
            val sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val soundUriString = sharedPreferences.getString(Constants.PREFS_NOTIFICATION_SOUND_URI, "")

            val notificationUri: Uri = if (soundUriString.isNullOrEmpty()) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(soundUriString)
            }

            val ringtone: Ringtone = RingtoneManager.getRingtone(context, notificationUri)

            ringtone.play()

            // Stop the sound after 1 second
            Handler(Looper.getMainLooper()).postDelayed({
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            }, 1000) // 1 second
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default sound if there's an error
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                ringtone.play()
                Handler(Looper.getMainLooper()).postDelayed({
                    if (ringtone.isPlaying) {
                        ringtone.stop()
                    }
                }, 1000)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
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

    fun updateNotification(notificationId: Int, notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }
}
