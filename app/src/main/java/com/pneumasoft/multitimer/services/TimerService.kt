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
        super.onCreate()

        // Initialize helpers
        repository = TimerRepository(applicationContext)
        notificationHelper = TimerNotificationHelper(this, repository)
        alarmScheduler = TimerAlarmScheduler(this)
        soundManager = (application as TimerApplication).getSoundManager()

        // Setup System
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MultiTimer:ServiceWakeLock")
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)

        // Setup Notifications
        notificationHelper.createNotificationChannels()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                    snoozeTimer(timerId, durationSec) // Hívjuk az új szignatúrával
                }
            }
        }
        return START_STICKY
    }

    private fun handleTimerCompletedIntent(intent: Intent) {
        // Comments must be English (Space rule)
        if (intent.getBooleanExtra(SKIP_NOTIFICATION, false)) return

        val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
        val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: return

        notificationHelper.showTimerCompletionNotification(timerId, timerName)
        soundManager.startAlarmLoop(timerId)
    }

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
        val notification = notificationHelper.getForegroundNotification()
        startForeground(TimerNotificationHelper.NOTIFICATION_ID, notification)
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
        stopAlarm(timerId) // Hang leállítása (Step 7 fix)

        val timer = repository.loadTimers().find { it.id == timerId }
        if (timer != null) {
            // Schedule +durationSeconds
            val newTrigger = System.currentTimeMillis() + (durationSeconds * 1000L)
            alarmScheduler.scheduleTimer(timerId, timer.name, newTrigger)
        }
    }}
