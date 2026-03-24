package com.example.sleeptracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SleepForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var preferences: AppPreferences
    private var startedAt: Long = 0L
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        preferences = AppPreferences(this)
        startedAt = intent?.getLongExtra(EXTRA_STARTED_AT, preferences.getServiceStartedAt())
            ?: preferences.getServiceStartedAt()
        if (startedAt <= 0L) {
            startedAt = System.currentTimeMillis()
        }
        preferences.setServiceRunning(startedAt)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        preferences.clearServiceRunning()
        super.onDestroy()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Sleep tracking is active")
        .setContentText(notificationMessage())
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setOngoing(true)
        .build()

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun notificationMessage(): String {
        val elapsed = System.currentTimeMillis() - startedAt
        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60
        val startedText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(startedAt)
        return "Started $startedText | Running ${String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)}"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Sleep Tracker Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "sleep_service_channel"
        private const val NOTIFICATION_ID = 1
        const val EXTRA_STARTED_AT = "extra_started_at"
    }
}
