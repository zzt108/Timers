package com.pneumasoft.multitimer.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pneumasoft.multitimer.R

class TimerService : Service() {
    private val binder = LocalBinder()
    private val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("MultiTimer App")
        .setContentText("Timers running")
        .setSmallIcon(R.drawable.ic_timer)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val name = "Timer Service Channel"
        val descriptionText = "Channel for Timer Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}
