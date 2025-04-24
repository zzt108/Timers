package com.pneumasoft.multitimer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log

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
    }

    private fun saveTimerStates(throwable: Throwable) {
        // Implement state saving logic for crash recovery
    }
}