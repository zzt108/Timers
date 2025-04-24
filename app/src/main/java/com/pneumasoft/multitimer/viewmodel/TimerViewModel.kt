package com.pneumasoft.multitimer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pneumasoft.multitimer.model.TimerItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers

    private val activeTimers = mutableMapOf<String, Job>()
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

    // In TimerViewModel.kt
    fun startTimer(id: String) {
        try {
            val timer = _timers.value.find { it.id == id } ?: return
            if (timer.isRunning) return

            // Update timer state to running
            _timers.value = _timers.value.map {
                if (it.id == id) it.copy(isRunning = true) else it
            }

            // Start the timer flow
            val job = viewModelScope.launch {
                try {
                    createTimerFlow(timer.remainingSeconds)
                        .collect { remainingSeconds ->
                            _timers.value = _timers.value.map {
                                if (it.id == id) it.copy(remainingSeconds = remainingSeconds) else it
                            }

                            if (remainingSeconds == 0) {
                                // Timer completed
                                _timers.value = _timers.value.map {
                                    if (it.id == id) it.copy(isRunning = false, completionTimestamp = System.currentTimeMillis()) else it
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("TimerViewModel", "Error in timer flow: ${e.message}", e)
                    // Reset timer state on error
                    _timers.value = _timers.value.map {
                        if (it.id == id) it.copy(isRunning = false) else it
                    }
                }
            }

            activeTimers[id] = job
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Failed to start timer: ${e.message}", e)
        }
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
