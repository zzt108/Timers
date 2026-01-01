package com.pneumasoft.multitimer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.pneumasoft.multitimer.databinding.ActivityAlarmBinding
import com.pneumasoft.multitimer.receivers.TimerAlarmReceiver
import com.pneumasoft.multitimer.services.TimerService

class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private var timerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Initialize binding
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Extract timer info from intent
        timerId = intent.getStringExtra(TimerAlarmReceiver.EXTRA_TIMER_ID)
        val timerName = intent.getStringExtra(TimerAlarmReceiver.EXTRA_TIMER_NAME) ?: "Timer"

        // Display timer name
        binding.alarmTimerName.text = timerName

        // Setup buttons
        binding.dismissButton.setOnClickListener {
            stopAlarm()
            finish()
        }

        binding.snoozeShortButton.setOnClickListener {
            snoozeAlarm(30L) // 30 seconds
            finish()
        }

        binding.snoozeLongButton.setOnClickListener {
            snoozeAlarm(60L) // 60 seconds
            finish()
        }
    }

    private fun stopAlarm() {
        timerId?.let { id ->
            val intent = Intent(this, TimerService::class.java).apply {
                action = TimerAlarmReceiver.ACTION_STOP_ALARM_LOOP
                putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, id)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun snoozeAlarm(durationSeconds: Long) {
        timerId?.let { id ->
            val intent = Intent(this, TimerService::class.java).apply {
                action = TimerAlarmReceiver.ACTION_SNOOZE_TIMER
                putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, id)
                putExtra(TimerAlarmReceiver.EXTRA_SNOOZE_DURATION, durationSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
