package com.pneumasoft.multitimer.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pneumasoft.multitimer.receivers.TimerAlarmReceiver

class TimerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val pendingIntents = HashMap<String, PendingIntent>()

    fun scheduleTimer(timerId: String, timerName: String, triggerAtMillis: Long) {
        val now = System.currentTimeMillis()
        val triggerTime = maxOf(triggerAtMillis, now)

        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_TIMER_COMPLETE
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_NAME, timerName)
        }

        val requestCode = timerId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntents[timerId] = pendingIntent

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    Log.w("TimerAlarmScheduler", "Exact alarm permission missing, falling back")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        } catch (e: SecurityException) {
            Log.e("TimerAlarmScheduler", "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e("TimerAlarmScheduler", "Error scheduling alarm: ${e.message}")
        }
    }

    fun cancelTimer(timerId: String) {
        try {
            pendingIntents[timerId]?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel() // Also cancel the PendingIntent itself
            }
            pendingIntents.remove(timerId)
            Log.d("TimerAlarmScheduler", "Alarm canceled for $timerId")
        } catch (e: Exception) {
            Log.e("TimerAlarmScheduler", "Failed to cancel alarm for $timerId", e)
        }
    }
}
