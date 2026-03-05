package com.example.sleeptracker

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class HomeActivity : AppCompatActivity() {

    private val channelId = "sleep_tracker_notifications"
    private val emergencyNumber = "9095207857"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        createNotificationChannel()

        // --- Bottom Navigation Setup ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home Screen", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_team -> {
                    val intent = Intent(this, TeamActivity::class.java)
                    intent.putExtra("type", "Team Members")
                    startActivity(intent)
                    true
                }
                R.id.nav_about -> {
                    val intent = Intent(this, TeamActivity::class.java)
                    intent.putExtra("type", "About Us")
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // --- UI Buttons ---
        val btnBirthday = findViewById<Button>(R.id.btnBirthday)
        val btnLocation = findViewById<Button>(R.id.btnLocation)
        val btnSendSMS = findViewById<Button>(R.id.btnSendSMS)
        val btnStartService = findViewById<Button>(R.id.btnStartService)
        val btnStopService = findViewById<Button>(R.id.btnStopService)

        btnBirthday.setOnClickListener { showDateTimePicker() }
        btnLocation.setOnClickListener { getGeoLocation() }
        btnSendSMS.setOnClickListener { sendEmergencySMS() }

        btnStartService.setOnClickListener {
            val serviceIntent = Intent(this, SleepForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Sleep Tracking Started", Toast.LENGTH_SHORT).show()
        }

        btnStopService.setOnClickListener {
            stopService(Intent(this, SleepForegroundService::class.java))
            Toast.makeText(this, "Sleep Tracking Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    // --- PICKER LOGIC ---
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val selectedDetails = "Birthday set: $day/${month + 1}/$year at $hour:$minute"
                Toast.makeText(this, selectedDetails, Toast.LENGTH_LONG).show()
                sendNotification("Birthday Reminder Set", "Alert configured for $day/${month+1}")
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    // --- LOCATION LOGIC ---
    private fun getGeoLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location != null) {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addressLine = addresses?.get(0)?.getAddressLine(0) ?: "Address not found"
                Toast.makeText(this, "Current Location: $addressLine", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Geocoder Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- SMS LOGIC ---
    private fun sendEmergencySMS() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Sending Alert")
        progressDialog.setMessage("Please wait...")
        progressDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(emergencyNumber, null, "Emergency Alert from Sleep Tracker!", null, null)
                progressDialog.dismiss()
                Toast.makeText(this, "SMS Sent", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this, "SMS Failed", Toast.LENGTH_SHORT).show()
            }
        }, 2000)
    }

    // --- MENU LOGIC ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(this, TeamActivity::class.java)
        when (item.itemId) {
            R.id.menu_about -> intent.putExtra("type", "About Us")
            R.id.menu_team -> intent.putExtra("type", "Team Details")
            R.id.menu_members -> intent.putExtra("type", "Team Members")
            R.id.menu_project -> intent.putExtra("type", "Project Description")
            R.id.menu_exit -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
        startActivity(intent)
        return true
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Close Sleep Tracker?")
            .setPositiveButton("Yes") { _, _ -> finishAffinity() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sleep Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}