package com.pneumasoft.multitimer.repository

import android.content.Context
import com.pneumasoft.multitimer.data.TimerDataHelper
import com.pneumasoft.multitimer.model.TimerItem

class TimerRepository(context: Context) {
    private val dataHelper = TimerDataHelper(context)

    fun saveTimers(timers: List<TimerItem>) {
        dataHelper.saveTimers(timers)
    }

    fun loadTimers(): List<TimerItem> {
        return dataHelper.loadTimers()
    }

    fun createDefaultTimersIfNeeded(): List<TimerItem> {
        if (!dataHelper.hasCreatedDefaultTimers()) {
            val defaultTimers = createDefaultTimers()
            dataHelper.saveTimers(defaultTimers)
            dataHelper.markDefaultTimersCreated()
            return defaultTimers
        }
        return emptyList()
    }

    private fun createDefaultTimers(): List<TimerItem> {
        return listOf(
            TimerItem(
                name = "Energy",
                durationSeconds = 4 * 3600 + 55 * 60,
                remainingSeconds = 4 * 3600 + 55 * 60
            ),
            TimerItem(
                name = "New race",
                durationSeconds = 15 * 60,
                remainingSeconds = 15 * 60
            ),
            TimerItem(
                name = "Nerve",
                durationSeconds = 4 * 3600,
                remainingSeconds = 4 * 3600
            )
        )
    }
}
