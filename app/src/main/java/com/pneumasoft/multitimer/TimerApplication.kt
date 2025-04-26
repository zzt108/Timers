package com.pneumasoft.multitimer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class TimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Register lifecycle callbacks to track app state
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activeActivities = 0

            override fun onActivityStarted(activity: Activity) {
                if (activeActivities == 0) {
                    // App going to foreground
                    Log.d("TIMER_APP", "Application entering foreground")
                }
                activeActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                activeActivities--
                if (activeActivities == 0) {
                    // App going to background
                    Log.d("TIMER_APP", "Application entering background")
                }
            }

            // Implement other required methods
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // Set uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TIMER_APP", "Uncaught exception in thread $thread", throwable)
            // Save active timer states for potential recovery
            saveTimerStates(throwable)
        }

        ApllySavedThemePreference()
    }

    private fun ApllySavedThemePreference(){
        // Apply saved theme preference
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = prefs.getString("dark_mode", "system") ?: "system"

        val mode = when (darkMode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                } else {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
            }
        }
        AppCompatDelegate.setDefaultNightMode(mode)

    }

    private fun saveTimerStates(throwable: Throwable) {
        // Implement state saving logic for crash recovery
    }
}