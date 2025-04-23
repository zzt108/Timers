package com.pneumasoft.multitimer

// Create app/src/main/java/com/pneumasoft/multitimer/TimerApplication.kt

import android.app.Application
import android.util.Log

class TimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set default exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TIMER_CRASH", "Uncaught exception in thread $thread", throwable)
        }
    }
}
