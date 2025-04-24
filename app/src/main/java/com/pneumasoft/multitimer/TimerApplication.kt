package com.pneumasoft.multitimer

import android.app.Application
import android.util.Log

class TimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set default exception handler to catch and log uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TIMER_APP", "Uncaught exception in thread $thread", throwable)
            // You could also save the exception to a file for later analysis
        }
    }
}
