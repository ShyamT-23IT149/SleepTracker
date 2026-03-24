package com.example.sleeptracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.PopupMenu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences
    private lateinit var bedtimeValue: TextView
    private lateinit var wakeupValue: TextView
    private lateinit var todaySummary: TextView
    private lateinit var locationValue: TextView
    private lateinit var userHeadline: TextView
    private lateinit var sleepPreferenceValue: TextView
    private lateinit var serviceStatusValue: TextView
    private lateinit var serviceStartedValue: TextView
    private var selectedBedtime: Calendar = Calendar.getInstance()
    private var mediaPlayer: MediaPlayer? = null
    private var playlistIndex = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private val serviceTimerRunnable = object : Runnable {
        override fun run() {
            updateServiceStatus()
            timerHandler.postDelayed(this, 1000L)
        }
    }

    private val playlistUris by lazy {
        listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)
        if (!preferences.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)
        createNotificationChannel()
        val toolbar = findViewById<Toolbar>(R.id.toolbarHome)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<ImageButton>(R.id.btnMoreOptions).setOnClickListener { anchor ->
            showPopupMenu(anchor)
        }

        val profile = preferences.getUserProfile()
        userHeadline = findViewById(R.id.tvUserHeadline)
        sleepPreferenceValue = findViewById(R.id.tvSleepPreference)
        bedtimeValue = findViewById(R.id.tvBedtimeValue)
        wakeupValue = findViewById(R.id.tvWakeValue)
        todaySummary = findViewById(R.id.tvTodaySummary)
        locationValue = findViewById(R.id.tvLocationValue)
        serviceStatusValue = findViewById(R.id.tvServiceStatusValue)
        serviceStartedValue = findViewById(R.id.tvServiceStartedValue)

        val pickBedtimeButton = findViewById<Button>(R.id.btnPickBedtime)
        val saveSleepButton = findViewById<Button>(R.id.btnSaveToday)
        val historyButton = findViewById<Button>(R.id.btnHistory)
        val profileButton = findViewById<Button>(R.id.btnEditProfile)
        val locationButton = findViewById<Button>(R.id.btnLocation)
        val smsButton = findViewById<Button>(R.id.btnSendSMS)
        val startServiceButton = findViewById<Button>(R.id.btnStartService)
        val stopServiceButton = findViewById<Button>(R.id.btnStopService)
        val playButton = findViewById<Button>(R.id.btnPlayPlaylist)
        val nextButton = findViewById<Button>(R.id.btnNextTrack)

        userHeadline.text = "Hello, ${profile.name.ifBlank { preferences.getSessionUsername() }}"
        sleepPreferenceValue.text = "Preferred sleep: ${profile.sleepHours} hours"

        loadTodayLog()

        pickBedtimeButton.setOnClickListener { showBedtimePicker() }
        saveSleepButton.setOnClickListener { saveTodaySleep(profile) }
        historyButton.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        profileButton.setOnClickListener { startActivity(Intent(this, ProfileSetupActivity::class.java)) }
        locationButton.setOnClickListener { fetchCurrentLocation() }
        smsButton.setOnClickListener { sendEmergencySms(profile.phone) }
        startServiceButton.setOnClickListener { startTrackingService() }
        stopServiceButton.setOnClickListener { stopTrackingService() }
        playButton.setOnClickListener { playCurrentTrack() }
        nextButton.setOnClickListener { playNextTrack() }
        updateServiceStatus()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.menu_login_page -> {
                preferences.clearLoginSession()
                stopTrackingService(showToast = false)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.menu_about -> openTeamPage("About Us")
            R.id.menu_team -> openTeamPage("Team Details")
            R.id.menu_members -> openTeamPage("Team Members")
            R.id.menu_member_profiles -> openTeamPage("Member Profiles")
            R.id.menu_project -> openTeamPage("Project Description")
            R.id.menu_logout -> {
                preferences.clearLoginSession()
                stopTrackingService()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit Sleep Tracker")
            .setMessage("Do you want to close the application?")
            .setPositiveButton("Yes") { _, _ -> finishAffinity() }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(serviceTimerRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        timerHandler.removeCallbacks(serviceTimerRunnable)
        timerHandler.post(serviceTimerRunnable)
    }

    override fun onPause() {
        timerHandler.removeCallbacks(serviceTimerRunnable)
        super.onPause()
    }

    private fun loadTodayLog() {
        val todayEntry = preferences.getTodaySleepLog()
        if (todayEntry != null) {
            bedtimeValue.text = todayEntry.bedtime
            wakeupValue.text = todayEntry.recommendedWakeup
            todaySummary.text = buildSummary(todayEntry)
        } else {
            todaySummary.text = "No sleep log saved for today yet."
        }
    }

    private fun showBedtimePicker() {
        val current = selectedBedtime
        android.app.TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedBedtime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                bedtimeValue.text = formatTime(selectedBedtime)
                wakeupValue.text = calculateRecommendedWakeup(preferences.getUserProfile().sleepHours)
                Toast.makeText(this, "Bedtime selected", Toast.LENGTH_SHORT).show()
            },
            current.get(Calendar.HOUR_OF_DAY),
            current.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun saveTodaySleep(profile: UserProfile) {
        if (bedtimeValue.text == getString(R.string.not_set)) {
            Toast.makeText(this, "Pick bedtime first", Toast.LENGTH_SHORT).show()
            return
        }

        val locationNote = locationValue.text.toString().takeIf { it.isNotBlank() && it != getString(R.string.location_not_checked) }.orEmpty()
        val entry = SleepModel(
            dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
            dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
            bedtime = bedtimeValue.text.toString(),
            recommendedWakeup = wakeupValue.text.toString(),
            sleepHours = profile.sleepHours,
            actualTrackedDuration = preferences.getTodaySleepLog()?.actualTrackedDuration.orEmpty(),
            note = if (locationNote.isBlank()) {
                "Today's sleep plan saved."
            } else {
                "Sleep place: $locationNote"
            }
        )
        preferences.saveTodaySleepLog(entry)
        todaySummary.text = buildSummary(entry)
        sendNotification("Today's sleep saved", "Wake up at ${entry.recommendedWakeup}")
        Toast.makeText(this, "Today's sleep log saved", Toast.LENGTH_SHORT).show()
    }

    private fun calculateRecommendedWakeup(hours: Int): String {
        val wakeup = selectedBedtime.clone() as Calendar
        wakeup.add(Calendar.HOUR_OF_DAY, hours)
        return formatTime(wakeup)
    }

    private fun fetchCurrentLocation() {
        val permissionsGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (location == null) {
            Toast.makeText(this, "Location unavailable right now", Toast.LENGTH_SHORT).show()
            return
        }

        runCatching {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val addressText = addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
            locationValue.text = addressText
            preferences.addHistoryEntry("Location checked: $addressText")
            Toast.makeText(this, "Location fetched", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Unable to decode location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmergencySms(phoneNumber: String) {
        val targetNumber = phoneNumber.ifBlank { preferences.getUserProfile().phone }
        if (targetNumber.isBlank()) {
            Toast.makeText(this, "Save a phone number in profile first", Toast.LENGTH_LONG).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS)
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Sending alert")
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        runCatching {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                targetNumber,
                null,
                "Sleep Tracker alert from ${preferences.getUserProfile().name.ifBlank { "user" }}. Please check in.",
                null,
                null
            )
            progressDialog.dismiss()
            preferences.addHistoryEntry("Emergency SMS sent to $targetNumber")
            Toast.makeText(this, "SMS sent", Toast.LENGTH_SHORT).show()
        }.onFailure {
            progressDialog.dismiss()
            Toast.makeText(this, "SMS failed to send", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTrackingService() {
        val startedAt = System.currentTimeMillis()
        preferences.setServiceRunning(startedAt)
        val serviceIntent = Intent(this, SleepForegroundService::class.java)
        serviceIntent.putExtra(SleepForegroundService.EXTRA_STARTED_AT, startedAt)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        preferences.addHistoryEntry("Foreground sleep tracking started")
        sendNotification("Sleep tracking active", "Foreground service started")
        updateServiceStatus()
        Toast.makeText(this, "Sleep tracking service started", Toast.LENGTH_SHORT).show()
    }

    private fun stopTrackingService(showToast: Boolean = true) {
        val startedAt = preferences.getServiceStartedAt()
        stopService(Intent(this, SleepForegroundService::class.java))
        preferences.clearServiceRunning()
        val todayEntry = preferences.getTodaySleepLog()
        if (todayEntry != null && startedAt > 0L) {
            val elapsedText = formatElapsed(System.currentTimeMillis() - startedAt)
            preferences.saveTodaySleepLog(
                todayEntry.copy(
                    actualTrackedDuration = elapsedText,
                    note = "${todayEntry.note} Tracked until stop: $elapsedText."
                )
            )
            loadTodayLog()
        }
        preferences.addHistoryEntry("Foreground sleep tracking stopped")
        updateServiceStatus()
        if (showToast) {
            Toast.makeText(this, "Sleep tracking service stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playCurrentTrack() {
        if (playlistUris.isEmpty()) {
            Toast.makeText(this, "No playlist source available", Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, playlistUris[playlistIndex])?.apply {
            setOnCompletionListener { playNextTrack() }
            start()
        }
        Toast.makeText(this, "Playing sleep playlist track ${playlistIndex + 1}", Toast.LENGTH_SHORT).show()
    }

    private fun playNextTrack() {
        if (playlistUris.isEmpty()) {
            return
        }
        playlistIndex = (playlistIndex + 1) % playlistUris.size
        playCurrentTrack()
    }

    private fun buildSummary(entry: SleepModel): String {
        val trackedSuffix = if (entry.actualTrackedDuration.isBlank()) {
            ""
        } else {
            " Tracked: ${entry.actualTrackedDuration}."
        }
        return "${entry.dateLabel}: Bed at ${entry.bedtime}, wake at ${entry.recommendedWakeup}, goal ${entry.sleepHours} hours.$trackedSuffix"
    }

    private fun openTeamPage(type: String): Boolean {
        startActivity(Intent(this, TeamActivity::class.java).putExtra("type", type))
        return true
    }

    private fun sendNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Sleep Tracker Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(calendar: Calendar): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
    }

    private fun updateServiceStatus() {
        if (!preferences.isServiceRunning()) {
            serviceStatusValue.text = "Not running"
            serviceStartedValue.text = "Start time: --"
            return
        }

        val startedAt = preferences.getServiceStartedAt()
        if (startedAt <= 0L) {
            serviceStatusValue.text = "Not running"
            serviceStartedValue.text = "Start time: --"
            return
        }

        val startedText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(startedAt)
        val elapsed = System.currentTimeMillis() - startedAt
        serviceStatusValue.text = "Running for ${formatElapsed(elapsed)}"
        serviceStartedValue.text = "Started at: $startedText"
    }

    private fun formatElapsed(elapsedMillis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun showPopupMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_main, menu)
            setOnMenuItemClickListener { handlePopupMenuItem(it) }
            show()
        }
    }

    private fun handlePopupMenuItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_login_page -> {
                preferences.clearLoginSession()
                stopTrackingService(showToast = false)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.menu_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.menu_about -> openTeamPage("About Us")
            R.id.menu_team -> openTeamPage("Team Details")
            R.id.menu_members -> openTeamPage("Team Members")
            R.id.menu_member_profiles -> openTeamPage("Member Profiles")
            R.id.menu_project -> openTeamPage("Project Description")
            R.id.menu_logout -> {
                preferences.clearLoginSession()
                stopTrackingService(showToast = false)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> false
        }
    }

    companion object {
        private const val CHANNEL_ID = "sleep_tracker_notifications"
        private const val REQUEST_LOCATION = 101
        private const val REQUEST_SMS = 102
        private const val REQUEST_NOTIFICATIONS = 103
    }
}
