// app/src/main/java/com/pneumasoft/multitimer/receivers/TimerAlarmReceiver.kt

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
        // Acquire wake lock to ensure we can complete processing
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MultiTimer:TimerWakeLock"
        )
        wakeLock.acquire(10*60*1000L) // 10 minutes max

        try {
            if (intent.action == ACTION_TIMER_COMPLETE) {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
                val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: return

                // Forward to service to handle notification
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.TIMER_COMPLETED_ACTION
                    putExtra(TimerService.EXTRA_TIMER_ID, timerId)
                    putExtra(TimerService.EXTRA_TIMER_NAME, timerName)
                }

                // Start service explicitly (needed when app might be in background)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Also broadcast locally for any active components
                LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(Intent(TimerService.TIMER_COMPLETED_ACTION).apply {
                        putExtra(TimerService.EXTRA_TIMER_ID, timerId)
                        putExtra(TimerService.EXTRA_TIMER_NAME, timerName)
                    })
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    companion object {
        const val ACTION_TIMER_COMPLETE = "com.pneumasoft.multitimer.ACTION_TIMER_COMPLETE"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_TIMER_NAME = "timer_name"
    }
}
