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
import com.pneumasoft.multitimer.model.TimerItem
import com.pneumasoft.multitimer.receivers.TimerAlarmReceiver
import com.pneumasoft.multitimer.repository.TimerRepository
import kotlinx.coroutines.*


class TimerService : Service() {
    private val binder = LocalBinder()
    private lateinit var repository: TimerRepository
    private lateinit var alarmManager: AlarmManager
    private val pendingIntents = HashMap<String, PendingIntent>()
    private var wakeLock: PowerManager.WakeLock? = null

    private var notificationUpdateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Load active timers from repository
        val activeTimers = repository.loadTimers().filter { it.isRunning }

        val contentText = when {
            activeTimers.isEmpty() -> "No active timers"
            activeTimers.size == 1 -> {
                val timer = activeTimers.first()
                // JAVÍTÁS: Újraszámolás
                val remaining = calculateRemainingSeconds(timer)
                "${timer.name}: ${formatTime(remaining)}"
            }
            else -> "${activeTimers.size} timers running"
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
        if (activeTimers.size > 1) {
            val timerList = activeTimers.joinToString("\n") { timer ->
                // JAVÍTÁS: Újraszámolás itt is
                val remaining = calculateRemainingSeconds(timer)
                "• ${timer.name}: ${formatTime(remaining)}"
            }
            bigTextStyle.bigText(timerList)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
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

    // NEW: Calculate real-time remaining seconds
    private fun calculateRemainingSeconds(timer: TimerItem): Int {
        if (!timer.isRunning || timer.absoluteEndTimeMillis == null) {
            return timer.remainingSeconds
        }
        val now = System.currentTimeMillis()
        val end = timer.absoluteEndTimeMillis!!
        val diff = (end - now) / 1000
        return diff.toInt().coerceAtLeast(0)
    }

    // helper method for time formatting
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

    /* never used
    private fun updateNotification() {
        // Create a new notification with updated timer count
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    */

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

    // MODIFY: In startForegroundService method (lines ~100-120)
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startNotificationUpdates() // NEW: Start the update loop
    }

    // start periodic updates
    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = serviceScope.launch {
            while (isActive) {
                try {
                    updateForegroundNotification()
                    delay(1000) // Update every 1 second
                } catch (e: Exception) {
                    Log.e("TimerService", "Notification update error: ${e.message}")
                }
            }
        }
    }

    // NEW: Method to update notification
    private fun updateForegroundNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // NEW: Method to stop updates
    private fun stopNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
    }

    // Public methods

    fun scheduleTimer(timerId: String, timerName: String, triggerAtMillis: Long) {
        val now = System.currentTimeMillis()
        val triggerTime = maxOf(triggerAtMillis, now)

        val intent = Intent(this, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_TIMER_COMPLETE
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_NAME, timerName)
        }

        val requestCode = timerId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntents[timerId] = pendingIntent

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ (Android 12+): Ellenőrizni kell a jogosultságot
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // FALLBACK: Ha nincs meg a jog, használjunk nem-pontos alarmot vagy logoljunk
                    // Jobb megoldás: setExact helyett setAndAllowWhileIdle (ami kevésbé pontos, de nem dob kivételt),
                    // VAGY értesítsük a felhasználót (de ez service-ben nehéz).
                    // Most a crash elkerülése a cél:
                    Log.w("TimerService", "Exact alarm permission missing, falling back to inexact alarm")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23-30: setExactAndAllowWhileIdle szabadon hívható (Doze mód miatt kell)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // API < 23: setExact
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Ez a catch blokk fogja meg, ha valamiért mégis SecurityException jönne
            Log.e("TimerService", "SecurityException scheduling alarm: ${e.message}")
            // Fallback: sima set (nem exact)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

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
        stopNotificationUpdates() // Stop the update loop
        notificationUpdateJob?.cancel()
        serviceScope.cancel() // Cancel the service scope
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
