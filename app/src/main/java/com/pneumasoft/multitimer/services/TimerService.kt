package com.pneumasoft.multitimer.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pneumasoft.multitimer.MainActivity
import com.pneumasoft.multitimer.R
import com.pneumasoft.multitimer.receivers.TimerAlarmReceiver
import com.pneumasoft.multitimer.repository.TimerRepository

class TimerService : Service() {
    private val binder = LocalBinder()
    private lateinit var repository: TimerRepository
    private lateinit var alarmManager: AlarmManager
    private val pendingIntents = HashMap<String, PendingIntent>()
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    // Add this broadcast receiver for timer completions
    private val timerCompletionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TIMER_COMPLETED_ACTION) {
                // Skip notification if flag is present
                if (intent.getBooleanExtra("SKIP_NOTIFICATION", false)) {
                    return
                }

                val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
                val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: return

                showTimerCompletionNotification(timerId, timerName)
            }
        }
    }

    private val systemEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Refresh foreground status when screen turns off
                    updateForegroundNotification()
                }
                Intent.ACTION_BATTERY_LOW -> {
                    // Alert user that timer might be killed on low battery
                    updateForegroundNotification("Timer may stop due to low battery")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Service Channel"
            val descriptionText = "Channel for Timer Service"
            val importance = NotificationManager.IMPORTANCE_LOW // Lower importance
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null) // Explicitly make it silent
                enableVibration(false) // Disable vibration too
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Alarms"
            val descriptionText = "Notifications for completed timers"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(ALARM_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Configure the notification channel with alarm sound
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmSound, audioAttributes)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String? = null): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MultiTimer Running")
            .setContentText(message ?: "Managing your timers")
            .setSmallIcon(R.drawable.ic_timer_simple)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun updateNotification() {
        // Create a new notification with updated timer count
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createTimerCompletionNotification(timerName: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get the default alarm sound
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        return NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle("$timerName elapsed")
            .setContentText("Timer $timerName has completed")
            .setSmallIcon(R.drawable.ic_timer_simple)
            .setContentIntent(pendingIntent)
            .setSound(alarmSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
    }

    private fun showTimerCompletionNotification(timerId: String, timerName: String) {
        // Create unique notification ID based on timer ID
        val notificationId = timerId.hashCode()
        val notification = createTimerCompletionNotification(timerName)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun registerReceivers() {
        // Monitor for potential system kills
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_BATTERY_LOW)
        }

        registerReceiver(systemEventReceiver, filter)
    }

    private fun updateForegroundNotification(message: String? = null) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MultiTimer Running")
            .setContentText("Managing your timers")
            .setSmallIcon(R.drawable.ic_timer_simple)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // Public methods

    fun scheduleTimer(timerId: String, timerName: String, durationMillis: Long) {
        val triggerTime = System.currentTimeMillis() + durationMillis

        // Create intent for alarm
        val intent = Intent(this, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_TIMER_COMPLETE
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_NAME, timerName)
        }

        // Create unique request code based on timer ID
        val requestCode = timerId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Store for cancellation
        pendingIntents[timerId] = pendingIntent

        // Use AlarmManager method that works in Doze mode
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to schedule alarm", e)
        }
    }

    fun cancelTimer(timerId: String) {
        try {
            pendingIntents[timerId]?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntents.remove(timerId)
                Log.d("TimerService", "Alarm canceled for timer $timerId")
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to cancel alarm for timer $timerId", e)
        }
    }

    // Service lifecycle methods

    override fun onCreate() {
        super.onCreate()

        // Acquire partial wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MultiTimer::ServiceWakeLock"
        )
        wakeLock?.acquire()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        repository = TimerRepository(applicationContext)
        createNotificationChannel()
        createAlarmNotificationChannel() // Create the alarm channel
        startForegroundService()
        registerReceivers()

        // Register receiver for timer completions
        LocalBroadcastManager.getInstance(this).registerReceiver(
            timerCompletionReceiver,
            IntentFilter(TIMER_COMPLETED_ACTION)
        )
    }

    override fun onDestroy() {
        // Release wake lock when service is destroyed
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null

        super.onDestroy()
        // Unregister timer completion receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerCompletionReceiver)

        // Unregister system event receiver
        try {
            unregisterReceiver(systemEventReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App swiped away from recents - consider saving timer state
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val ALARM_CHANNEL_ID = "TimerAlarmChannel"
        const val NOTIFICATION_ID = 1
        const val TIMER_COMPLETED_ACTION = "com.pneumasoft.multitimer.TIMER_COMPLETED"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_TIMER_NAME = "timer_name"
    }
}
