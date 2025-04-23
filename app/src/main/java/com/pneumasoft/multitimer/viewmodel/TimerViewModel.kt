package com.pneumasoft.multitimer.viewmodel

import androidx.lifecycle.ViewModel
import com.pneumasoft.multitimer.model.TimerItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
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

    fun startTimer(id: String) {
        val timer = _timers.value.find { it.id == id } ?: return
        if (timer.isRunning) return

        // Mark timer as running
        _timers.value = _timers.value.map {
            if (it.id == id) it.copy(isRunning = true) else it
        }

        // Start a coroutine for this timer
        val job = viewModelScope.launch {
            // Create a flow that emits remaining seconds every second
            (timer.remainingSeconds downTo 0).asFlow()
                .onEach { delay(1000) } // Wait 1 second between emissions
                .collect { remainingSeconds ->
                    // Update the timer's remaining seconds
                    _timers.value = _timers.value.map {
                        if (it.id == id) {
                            it.copy(remainingSeconds = remainingSeconds)
                        } else {
                            it
                        }
                    }

                    // When timer completes
                    if (remainingSeconds == 0) {
                        _timers.value = _timers.value.map {
                            if (it.id == id) {
                                it.copy(
                                    isRunning = false,
                                    completionTimestamp = System.currentTimeMillis()
                                )
                            } else {
                                it
                            }
                        }
                        activeTimers.remove(id)
                    }
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
