package com.pneumasoft.multitimer.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.pneumasoft.multitimer.TimerApplication
import com.pneumasoft.multitimer.receivers.TimerAlarmReceiver
import com.pneumasoft.multitimer.repository.TimerRepository
import android.provider.Settings
import com.pneumasoft.multitimer.AlarmActivity
import android.util.Log
import kotlinx.coroutines.*

class TimerService : Service() {

    // Service bindings
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    // Helpers (Composition)
    private lateinit var repository: TimerRepository
    private lateinit var notificationHelper: TimerNotificationHelper
    private lateinit var alarmScheduler: TimerAlarmScheduler
    private lateinit var soundManager: TimerSoundManager

    // Jobs
    private var notificationUpdateJob: Job? = null

    companion object {
        const val TIMER_COMPLETED_ACTION = "com.pneumasoft.multitimer.TIMER_COMPLETED"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_TIMER_NAME = "timer_name"
        const val SKIP_NOTIFICATION = "skip_notification"
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val timerCompletionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TIMER_COMPLETED_ACTION) {
                if (intent.getBooleanExtra(SKIP_NOTIFICATION, false)) return

                val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
                val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: return

                // Use helpers
                notificationHelper.showTimerCompletionNotification(timerId, timerName)
                soundManager.startAlarmLoop(timerId)
            }
        }
    }

    override fun onCreate() {
        Log.d("TimerService", "onCreate called")
        super.onCreate()

        // Initialize helpers
        repository = TimerRepository(applicationContext)
        notificationHelper = TimerNotificationHelper(this, repository)
        alarmScheduler = TimerAlarmScheduler(this)
        
        // Use safe cast for TimerApplication
        val app = application as? TimerApplication
        soundManager = app?.soundManager ?: TimerSoundManager(this, serviceScope)

        // Setup System
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MultiTimer:ServiceWakeLock")
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)

        // Setup Notifications
        notificationHelper.createNotificationChannels()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TimerService", "onStartCommand called with action: ${intent?.action}")
        // Ensure foreground service is promoted immediately to avoid crashes
        startForegroundService()

        // Step 7: Handle Action Buttons from Notification
        when (intent?.action) {
            TIMER_COMPLETED_ACTION -> {
                handleTimerCompletedIntent(intent)
                return START_STICKY
            }
            TimerAlarmReceiver.ACTION_STOP_ALARM_LOOP -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                if (timerId != null) {
                    stopAlarm(timerId)
                }
            }
            TimerAlarmReceiver.ACTION_SNOOZE_TIMER -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                val durationSec = intent.getLongExtra(TimerAlarmReceiver.EXTRA_SNOOZE_DURATION, 300L)
                if (timerId != null) {
                    snoozeTimer(timerId, durationSec) // Call with the new signature
                }
            }
        }
        return START_STICKY
    }

    private fun handleTimerCompletedIntent(intent: Intent) {
        if (intent.getBooleanExtra(SKIP_NOTIFICATION, false)) return

        val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
        val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: return

        notificationHelper.showTimerCompletionNotification(timerId, timerName)
        soundManager.startAlarmLoop(timerId)

        // Force Activity Launch if allowed
    val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    Log.d("TimerService", "handleTimerCompletedIntent: canDrawOverlays=$canDraw, SDK_INT=${Build.VERSION.SDK_INT}")
    
    if (canDraw) {
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_TIMER_NAME, timerName)
        }
        Log.d("TimerService", "handleTimerCompletedIntent: Starting AlarmActivity")
        startActivity(activityIntent)
    } else {
        Log.d("TimerService", "handleTimerCompletedIntent: Skipping AlarmActivity launch (overlay permission missing)")
    }    }

    override fun onDestroy() {
        notificationUpdateJob?.cancel()
        soundManager.stopAll()
        serviceScope.cancel()

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder = binder

    // --- Public Methods delegated to helpers ---

    fun scheduleTimer(timerId: String, timerName: String, triggerAtMillis: Long) {
        alarmScheduler.scheduleTimer(timerId, timerName, triggerAtMillis)
    }

    fun cancelTimer(timerId: String) {
        alarmScheduler.cancelTimer(timerId)
    }

    // --- Internal Logic ---

    private fun startForegroundService() {
        Log.d("TimerService", "startForegroundService (internal) called")
        val notification = notificationHelper.getForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TimerNotificationHelper.NOTIFICATION_ID, 
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(TimerNotificationHelper.NOTIFICATION_ID, notification)
        }
        startNotificationUpdates()
    }


    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = serviceScope.launch {
            while (isActive) {
                try {
                    notificationHelper.updateForegroundNotification()
                    delay(1000)
                } catch (e: Exception) {
                    // Log error
                }
            }
        }
    }

    private fun stopAlarm(timerId: String) {
        soundManager.stopAlarmLoop(timerId)
        notificationHelper.cancelNotification(timerId.hashCode())
    }

    private fun snoozeTimer(timerId: String, durationSeconds: Long) {
        Log.d("TimerService", "snoozeTimer id=$timerId, duration=$durationSeconds")
        stopAlarm(timerId) // Stop sound (Step 7 fix)

        val allTimers = repository.loadTimers()
        val timer = allTimers.find { it.id == timerId }
        
        if (timer != null) {
            // Schedule +durationSeconds
            val newTrigger = System.currentTimeMillis() + (durationSeconds * 1000L)
            alarmScheduler.scheduleTimer(timerId, timer.name, newTrigger)
            
            // Update state
            timer.absoluteEndTimeMillis = newTrigger
            timer.remainingSeconds = durationSeconds.toInt()
            timer.isRunning = true
            timer.completionTimestamp = null // Clear any completion state
            
            repository.saveTimers(allTimers)
        } else {
            // Log.d("TimerService", "snoozeTimer: Timer $timerId not found!")
        }
    }
}
