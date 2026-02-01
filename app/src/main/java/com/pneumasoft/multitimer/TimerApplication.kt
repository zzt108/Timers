package com.pneumasoft.multitimer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.pneumasoft.multitimer.services.TimerSoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


class TimerApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val soundManager: TimerSoundManager by lazy {
        TimerSoundManager(this, appScope)
    }


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

        ApllySavedThemePreference()
    }


    private fun ApllySavedThemePreference(){
        // Apply the saved theme preference
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val themeMode = when (prefs.getString("theme_mode", "system")) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun saveTimerStates(throwable: Throwable) {
        // Implement state saving logic for crash recovery
    }
}