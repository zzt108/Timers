package com.pneumasoft.multitimer.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pneumasoft.multitimer.model.TimerItem
import java.lang.reflect.Type

class TimerDataHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFERENCES = "timer_preferences"
        private const val KEY_TIMERS = "saved_timers"
        private const val KEY_DEFAULT_TIMERS_CREATED = "default_timers_created"
    }

    fun saveTimers(timers: List<TimerItem>) {
        val timersJson = gson.toJson(timers)
        sharedPreferences.edit().putString(KEY_TIMERS, timersJson).apply()
    }

    fun loadTimers(): List<TimerItem> {
        val timersJson = sharedPreferences.getString(KEY_TIMERS, null) ?: return emptyList()
        val timerType: Type = object : TypeToken<List<TimerItem>>() {}.type
        val timers: List<TimerItem> = gson.fromJson(timersJson, timerType) ?: emptyList()

        // Migration logic: calculate absoluteEndTimeMillis for running timers if missing
        return timers.map { timer ->
            if (timer.isRunning && timer.absoluteEndTimeMillis == null) {
                // If it was running but has no end time (from old version),
                // we restart the calculation based on current time + remaining
                val calculatedEndTime = System.currentTimeMillis() + (timer.remainingSeconds * 1000L)
                timer.copy(absoluteEndTimeMillis = calculatedEndTime)
            } else {
                timer
            }
        }
    }

    fun hasCreatedDefaultTimers(): Boolean {
        return sharedPreferences.getBoolean(KEY_DEFAULT_TIMERS_CREATED, false)
    }

    fun markDefaultTimersCreated() {
        sharedPreferences.edit().putBoolean(KEY_DEFAULT_TIMERS_CREATED, true).apply()
    }
}
