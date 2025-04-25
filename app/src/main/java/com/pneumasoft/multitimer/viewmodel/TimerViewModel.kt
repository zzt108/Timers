package com.pneumasoft.multitimer.viewmodel

import android.app.Application
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pneumasoft.multitimer.services.TimerService

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimerRepository(application)

    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers

    private val activeTimers = mutableMapOf<String, Job>()

    // Add this method to your TimerViewModel class
    private fun sendTimerCompletedBroadcast(id: String, name: String) {
        val intent = Intent(TimerService.TIMER_COMPLETED_ACTION).apply {
            putExtra(TimerService.EXTRA_TIMER_ID, id)
            putExtra(TimerService.EXTRA_TIMER_NAME, name)
        }
        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(intent)
    }

    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob() +
            CoroutineExceptionHandler { _, exception ->
                Log.e("TimerViewModel", "Coroutine error: ${exception.message}", exception)
            })

    init {
        loadSavedTimers()
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

    fun addTimer(name: String, durationSeconds: Int) {
        val newTimer = TimerItem(
            name = name,
            durationSeconds = durationSeconds,
            remainingSeconds = durationSeconds
        )
        _timers.value = _timers.value + newTimer
        saveTimers()
    }

    fun updateTimer(id: String, name: String, durationSeconds: Int) {
        _timers.value = _timers.value.map { timer ->
            if (timer.id == id) {
                if (timer.isRunning) {
                    stopTimer(id)
                }
                timer.copy(
                    name = name,
                    durationSeconds = durationSeconds,
                    remainingSeconds = durationSeconds
                )
            } else {
                timer
            }
        }
        saveTimers()
    }

    fun startTimer(id: String) {
        // Existing code from the original file remains the same
        val timer = _timers.value.find { it.id == id } ?: return
        if (timer.isRunning) return

        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = true) else it
        }
        saveTimers()

        val job = viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                var lastTickTime = startTime

                while (isActive) {
                    val now = System.currentTimeMillis()
                    val elapsedSinceLastTick = now - lastTickTime

                    if (elapsedSinceLastTick >= 1000) {
                        lastTickTime = now

                        val currentTimer = _timers.value.find { it.id == id } ?: break
                        if (currentTimer.remainingSeconds <= 0) break

                        _timers.value = _timers.value.map {
                            if (it.id == id) {
                                val newRemaining = it.remainingSeconds - 1
                                if (newRemaining <= 0) {
                                    sendTimerCompletedBroadcast(id, it.name)
                                }
                                it.copy(
                                    remainingSeconds = newRemaining,
                                    isRunning = newRemaining > 0,
                                    completionTimestamp = if (newRemaining <= 0) System.currentTimeMillis() else null
                                )
                            } else it
                        }
                        saveTimers()
                    }
                    delay(100)
                }
            } catch (e: Exception) {
                Log.e("TimerViewModel", "Timer error: ${e.message}", e)
                _timers.value = _timers.value.map {
                    if (it.id == id) it.copy(isRunning = false) else it
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

    private fun stopTimer(id: String) {
        activeTimers[id]?.cancel()
        activeTimers.remove(id)
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = false) else it
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeTimers.values.forEach { it.cancel() }
        activeTimers.clear()
        saveTimers()
    }
}
