package com.pneumasoft.multitimer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.pneumasoft.multitimer.model.TimerItem
import com.pneumasoft.multitimer.repository.TimerRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.pneumasoft.multitimer.services.TimerService

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    // Properties
    private val repository = TimerRepository(application)
    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers
    private val activeTimers = mutableMapOf<String, Job>()
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob() +
            CoroutineExceptionHandler { _, exception ->
                Log.e("TimerViewModel", "Coroutine error: ${exception.message}", exception)
            })

    // Initialization
    init {
        loadSavedTimers()
    }

    // Private methods
// app/src/main/java/com/pneumasoft/multitimer/viewmodel/TimerViewModel.kt

// REMOVE (old):
// import androidx.localbroadcastmanager.content.LocalBroadcastManager

    private fun sendTimerCompletedBroadcast(id: String, name: String) {
        // Comments must be English (Space rule)
        val context = getApplication<Application>()

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.TIMER_COMPLETED_ACTION
            putExtra(TimerService.EXTRA_TIMER_ID, id)
            putExtra(TimerService.EXTRA_TIMER_NAME, name)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun loadSavedTimers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First try to load saved timers
                val savedTimers = repository.loadTimers()

                if (savedTimers.isNotEmpty()) {
                    _timers.value = savedTimers
                } else {
                    // If no saved timers found, create defaults
                    val defaultTimers = repository.createDefaultTimersIfNeeded()
                    if (defaultTimers.isNotEmpty()) {
                        _timers.value = defaultTimers
                    }
                }
            } catch (e: Exception) {
                Log.e("TimerViewModel", "Failed to load saved timers", e)
            }
        }
    }

    private fun saveTimers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveTimers(_timers.value)
            } catch (e: Exception) {
                Log.e("TimerViewModel", "Failed to save timers", e)
            }
        }
    }

    private fun stopTimer(id: String) {
        // Cancel coroutine job
        activeTimers[id]?.cancel()
        activeTimers.remove(id)

        // Update timer state
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = false) else it
        }

        // Cancel the alarm in TimerService
        cancelAlarmInService(id)
    }

    private fun cancelAlarmInService(timerId: String) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, TimerService::class.java)

        context.bindService(serviceIntent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as TimerService.LocalBinder
                val timerService = binder.getService()

                // Cancel the alarm for this timer
                timerService.cancelTimer(timerId)

                // Unbind from service
                context.unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Not needed
            }
        }, Context.BIND_AUTO_CREATE)
    }

    // Public methods
    fun addTimer(name: String, durationSeconds: Int) {
        val newTimer = TimerItem(
            name = name,
            durationSeconds = durationSeconds,
            remainingSeconds = durationSeconds
        )
        _timers.value = _timers.value + newTimer
        saveTimers()
    }

// app/src/main/java/com/pneumasoft/multitimer/viewmodel/TimerViewModel.kt

    fun updateTimer(id: String, name: String, durationSeconds: Int, editRemainingTime: Boolean = false) {
        // Find the timer
        val timer = _timers.value.find { it.id == id } ?: return
        val wasRunning = timer.isRunning

        // Pause the timer if it's running (we'll restart it at the end if needed)
        if (wasRunning) {
            pauseTimer(id)
        }

        // Update timer data
        _timers.value = _timers.value.map { t ->
            if (t.id == id) {
                if (editRemainingTime) {
                    // Editing remaining time (for running timers)
                    t.copy(
                        name = name,
                        remainingSeconds = durationSeconds, // Set new remaining time directly
                        isRunning = false  // Will be set back to true below if needed
                    )
                } else {
                    // Editing total duration (for stopped timers)
                    t.copy(
                        name = name,
                        durationSeconds = durationSeconds,
                        remainingSeconds = durationSeconds, // Reset remaining time to full duration
                        isRunning = false
                    )
                }
            } else {
                t
            }
        }

        // If timer was running before, restart it
        if (wasRunning) {
            startTimer(id)
        }

        saveTimers()
    }


    fun startTimer(id: String) {
        val timer = _timers.value.find { it.id == id } ?: return
        if (timer.isRunning) return

        // Calculate absolute end time
        val endTimeMillis = System.currentTimeMillis() + (timer.remainingSeconds * 1000L)

        _timers.value = _timers.value.map {
            if (it.id == id) {
                it.copy(
                    isRunning = true,
                    absoluteEndTimeMillis = endTimeMillis
                )
            } else it
        }
        saveTimers()

        // Get application context
        val context = getApplication<Application>()

        // If timer duration exceeds 1 minute, use AlarmManager for reliability
        if (timer.remainingSeconds > 60) {
            // Bind to service to access its methods
            val serviceIntent = Intent(context, TimerService::class.java)
            context.bindService(serviceIntent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as TimerService.LocalBinder
                    val timerService = binder.getService()

                    // Schedule using AlarmManager with absolute end time
                    timerService.scheduleTimer(
                        timerId = timer.id,
                        timerName = timer.name,
                        triggerAtMillis = endTimeMillis
                    )

                    context.unbindService(this)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    // Not needed
                }
            }, Context.BIND_AUTO_CREATE)
        }

        // Coroutine-based UI updates using absolute time calculation
        val job = viewModelScope.launch {
            try {
                while (isActive) {
                    val currentTimer = _timers.value.find { it.id == id } ?: break
                    val endTime = currentTimer.absoluteEndTimeMillis ?: break

                    // Calculate remaining time from absolute end time
                    val now = System.currentTimeMillis()
                    val remainingMillis = endTime - now
                    val remainingSeconds = (remainingMillis / 1000).toInt().coerceAtLeast(0)

                    if (remainingSeconds <= 0) {
                        // Timer completed
                        sendTimerCompletedBroadcast(id, currentTimer.name)
                        _timers.value = _timers.value.map {
                            if (it.id == id) {
                                it.copy(
                                    remainingSeconds = 0,
                                    isRunning = false,
                                    completionTimestamp = System.currentTimeMillis(),
                                    absoluteEndTimeMillis = null
                                )
                            } else it
                        }
                        saveTimers()
                        break
                    }

                    // Update remaining time
                    _timers.value = _timers.value.map {
                        if (it.id == id) {
                            it.copy(remainingSeconds = remainingSeconds)
                        } else it
                    }

                    delay(1000) // Now 1 sec delay is OK, because we always recalculate
                }
            } catch (e: Exception) {
                Log.e("TimerViewModel", "Timer error: ${e.message}", e)
                _timers.value = _timers.value.map {
                    if (it.id == id) it.copy(isRunning = false, absoluteEndTimeMillis = null) else it
                }
                saveTimers()
            }
        }

        activeTimers[id] = job
    }

    fun pauseTimer(id: String) {
        stopTimer(id)
        saveTimers()
    }

    fun resetTimer(id: String) {
        stopTimer(id)
        _timers.value = _timers.value.map {
            if (it.id == id) {
                it.copy(remainingSeconds = it.durationSeconds, completionTimestamp = null)
            } else {
                it
            }
        }
        saveTimers()
    }

    fun deleteTimer(id: String) {
        stopTimer(id)
        _timers.value = _timers.value.filterNot { it.id == id }
        saveTimers()
    }

    override fun onCleared() {
        super.onCleared()
        activeTimers.values.forEach { it.cancel() }
        activeTimers.clear()
        saveTimers()
    }
}
