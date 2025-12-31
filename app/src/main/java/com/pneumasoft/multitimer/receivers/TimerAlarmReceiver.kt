package com.pneumasoft.multitimer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pneumasoft.multitimer.services.TimerService

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Acquire wake lock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MultiTimer:TimerWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes max

        try {
            when (intent.action) {
                ACTION_TIMER_COMPLETE -> {
                    val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
                    val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: return

                    // Forward to service
                    val serviceIntent = Intent(context, TimerService::class.java).apply {
                        action = TimerService.TIMER_COMPLETED_ACTION
                        putExtra(TimerService.EXTRA_TIMER_ID, timerId)
                        putExtra(TimerService.EXTRA_TIMER_NAME, timerName)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    // Broadcast locally
                    LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(Intent(TimerService.TIMER_COMPLETED_ACTION).apply {
                            putExtra(TimerService.EXTRA_TIMER_ID, timerId)
                            putExtra(TimerService.EXTRA_TIMER_NAME, timerName)
                        })
                }

                ACTION_DISMISS_ALARM -> {
                    val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return

                    // Send intent to service to stop alarm loop
                    val serviceIntent = Intent(context, TimerService::class.java).apply {
                        action = ACTION_STOP_ALARM_LOOP
                        putExtra(EXTRA_TIMER_ID, timerId)
                    }
                    context.startService(serviceIntent)
                }

                ACTION_SNOOZE_ALARM -> {
                    val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return

                    // Load timer name from repository (or pass it through intent)
                    val serviceIntent = Intent(context, TimerService::class.java).apply {
                        action = ACTION_SNOOZE_TIMER
                        putExtra(EXTRA_TIMER_ID, timerId)
                    }
                    context.startService(serviceIntent)
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        const val ACTION_TIMER_COMPLETE = "com.pneumasoft.multitimer.ACTION_TIMER_COMPLETE"
        const val ACTION_DISMISS_ALARM = "com.pneumasoft.multitimer.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.pneumasoft.multitimer.ACTION_SNOOZE_ALARM"
        const val ACTION_STOP_ALARM_LOOP = "com.pneumasoft.multitimer.ACTION_STOP_ALARM_LOOP"
        const val ACTION_SNOOZE_TIMER = "com.pneumasoft.multitimer.ACTION_SNOOZE_TIMER"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_TIMER_NAME = "timer_name"
    }
}
