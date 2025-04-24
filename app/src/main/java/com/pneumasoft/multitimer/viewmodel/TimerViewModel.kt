package com.pneumasoft.multitimer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pneumasoft.multitimer.model.TimerItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    // Add persistence
    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers

    // Use a more robust scope with error handling
    private val activeTimers = mutableMapOf<String, Job>()
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob() +
            CoroutineExceptionHandler { _, exception ->
                Log.e("TimerViewModel", "Coroutine error: ${exception.message}", exception)
            })

    init {
        // Load saved timers on initialization
        loadSavedTimers()
    }

    private fun loadSavedTimers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Implementation for loading saved timers from preferences or database
            } catch (e: Exception) {
                Log.e("TimerViewModel", "Failed to load saved timers", e)
            }
        }
    }

    // Save timers when they change
    private fun saveTimers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Implementation for saving timers
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
    }

    fun updateTimer(id: String, name: String, durationSeconds: Int) {
        _timers.value = _timers.value.map { timer ->
            if (timer.id == id) {
                // If timer is running, stop it first
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
    }

    private fun createTimerFlow(durationSeconds: Int): Flow<Int> = flow {
        for (seconds in durationSeconds downTo 0) {
            emit(seconds)
            delay(1000)
        }
    }

    fun startTimer(id: String) {
        val timer = _timers.value.find { it.id == id } ?: return
        if (timer.isRunning) return

        // Update timer state to running
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

                        // Find current timer state
                        val currentTimer = _timers.value.find { it.id == id } ?: break
                        if (currentTimer.remainingSeconds <= 0) break

                        // Update timer
                        _timers.value = _timers.value.map {
                            if (it.id == id) {
                                val newRemaining = it.remainingSeconds - 1
                                it.copy(
                                    remainingSeconds = newRemaining,
                                    isRunning = newRemaining > 0,
                                    completionTimestamp = if (newRemaining <= 0) System.currentTimeMillis() else null
                                )
                            } else it
                        }
                        saveTimers()
                    }

                    // Use a small delay to avoid consuming too much CPU
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
    }

    fun deleteTimer(id: String) {
        stopTimer(id)
        _timers.value = _timers.value.filterNot { it.id == id }
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
        // Cancel all active timers when ViewModel is cleared
        activeTimers.values.forEach { it.cancel() }
        activeTimers.clear()
    }
}
