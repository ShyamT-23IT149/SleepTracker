package com.example.sleeptracker

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SleepForegroundService : Service() {

    private val CHANNEL_ID = "sleep_alerts"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create the notification that stays in the status bar
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sleep Tracking Active")
            .setContentText("Your sleep is being monitored in the background.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true) // Cannot be swiped away
            .build()

        // Starting foreground tells Android this is a high-priority task
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}