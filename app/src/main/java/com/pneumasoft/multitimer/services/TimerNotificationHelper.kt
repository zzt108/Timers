package com.pneumasoft.multitimer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.model.TimerItem
import com.pneumasoft.multitimer.receivers.TimerAlarmReceiver
import com.pneumasoft.multitimer.repository.TimerRepository

class TimerNotificationHelper(
    private val context: Context,
    private val repository: TimerRepository
) {
    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val ALARM_CHANNEL_ID = "TimerAlarmChannel"
        const val NOTIFICATION_ID = 1
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        // 1. Service Channel (Silent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Timer Service"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // 2. Alarm Channel (High Priority, Sound)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Timer Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for completed timers"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun getForegroundNotification(): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Load active timers for dynamic content
        val activeTimers = repository.loadTimers().filter { it.isRunning }

        val contentText = when {
            activeTimers.isEmpty() -> "No active timers"
            activeTimers.size == 1 -> {
                val timer = activeTimers.first()
                val remaining = calculateRemainingSeconds(timer)
                "${timer.name}: ${formatTime(remaining)}"
            }
            else -> "${activeTimers.size} timers running"
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
        if (activeTimers.size > 1) {
            val timerList = activeTimers.joinToString("\n") { timer ->
                val remaining = calculateRemainingSeconds(timer)
                "${timer.name}: ${formatTime(remaining)}"
            }
            bigTextStyle.bigText(timerList)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MultiTimer Running")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_timer_simple)
            .setContentIntent(pendingIntent)
            .setStyle(if (activeTimers.size > 1) bigTextStyle else null)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    fun updateForegroundNotification() {
        notificationManager.notify(NOTIFICATION_ID, getForegroundNotification())
    }

    // Step 7: Alarm Notification with Action Buttons
    fun showTimerCompletionNotification(timerId: String, timerName: String) {
        // Dismiss Intent
        val dismissIntent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Intent
        val snoozeIntent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            timerId.hashCode() + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full Screen Intent
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            timerId.hashCode() + 2,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setContentTitle("Timer Complete!")
            .setContentText(timerName)
            .setSmallIcon(R.drawable.ic_timer_simple)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            // Add Action Buttons
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze 5 min", snoozePendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 1000, 1000, 1000))
            .build()

        notificationManager.notify(timerId.hashCode(), notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun calculateRemainingSeconds(timer: TimerItem): Int {
        if (!timer.isRunning || timer.absoluteEndTimeMillis == null) return timer.remainingSeconds
        val now = System.currentTimeMillis()
        val end = timer.absoluteEndTimeMillis!!
        val diff = (end - now) / 1000
        return diff.toInt().coerceAtLeast(0)
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
}
