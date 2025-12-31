package com.pneumasoft.multitimer.services

import android.content.Context
import android.media.RingtoneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerSoundManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val alarmLoops = mutableMapOf<String, Job>()

    fun startAlarmLoop(timerId: String) {
        stopAlarmLoop(timerId) // Cancel existing if any

        alarmLoops[timerId] = scope.launch {
            // Repeat sound every 30 seconds, max 10 times (5 mins)
            repeat(10) {
                if (!isActive) return@launch

                playAlarmSound()
                delay(30000) // 30 seconds wait
            }
        }
    }

    fun stopAlarmLoop(timerId: String) {
        alarmLoops[timerId]?.cancel()
        alarmLoops.remove(timerId)
    }

    fun stopAll() {
        alarmLoops.values.forEach { it.cancel() }
        alarmLoops.clear()
    }

    private fun playAlarmSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
