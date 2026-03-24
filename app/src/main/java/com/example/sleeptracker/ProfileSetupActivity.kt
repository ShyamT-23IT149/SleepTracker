package com.example.sleeptracker

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences
    private var birthdayReminderAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)
        preferences = AppPreferences(this)

        val existingProfile = preferences.getUserProfile()
        birthdayReminderAt = existingProfile.birthdayReminderAt

        val nameInput = findViewById<EditText>(R.id.etName)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val phoneInput = findViewById<EditText>(R.id.etPhone)
        val sleepHoursInput = findViewById<EditText>(R.id.etSleepHours)
        val friendNameInput = findViewById<EditText>(R.id.etFriendName)
        val birthdayText = findViewById<TextView>(R.id.tvBirthdayValue)
        val chooseBirthdayButton = findViewById<Button>(R.id.btnPickBirthday)
        val continueButton = findViewById<Button>(R.id.btnSaveProfile)

        nameInput.setText(existingProfile.name)
        emailInput.setText(existingProfile.email)
        phoneInput.setText(existingProfile.phone)
        sleepHoursInput.setText(existingProfile.sleepHours.toString())
        friendNameInput.setText(existingProfile.friendName)
        birthdayText.text = formatReminderText(existingProfile.birthdayReminderAt)

        chooseBirthdayButton.setOnClickListener {
            openBirthdayPicker(birthdayText)
        }

        continueButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val sleepHours = sleepHoursInput.text.toString().toIntOrNull()
            val friendName = friendNameInput.text.toString().trim()

            if (name.isBlank() || email.isBlank() || phone.isBlank() || sleepHours == null || sleepHours !in 4..12) {
                Toast.makeText(this, "Fill all details and keep sleep hours between 4 and 12", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Saving profile")
            progressDialog.setMessage("Updating your sleep preferences...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            val profile = UserProfile(
                name = name,
                username = preferences.getSessionUsername(),
                email = email,
                phone = phone,
                sleepHours = sleepHours,
                friendName = friendName,
                birthdayReminderAt = birthdayReminderAt
            )
            preferences.saveUserProfile(profile)
            progressDialog.dismiss()

            if (birthdayReminderAt > System.currentTimeMillis() && friendName.isNotBlank()) {
                scheduleBirthdayReminder(friendName, birthdayReminderAt)
            }

            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun openBirthdayPicker(birthdayText: TextView) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val selected = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                        }
                        birthdayReminderAt = selected.timeInMillis
                        birthdayText.text = formatReminderText(birthdayReminderAt)
                        Toast.makeText(this, "Birthday reminder selected", Toast.LENGTH_SHORT).show()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun scheduleBirthdayReminder(friendName: String, triggerAtMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, "Birthday Reminder")
            putExtra(ReminderReceiver.EXTRA_MESSAGE, "Wish $friendName at the scheduled time.")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun formatReminderText(timestamp: Long): String {
        if (timestamp <= 0L) {
            return "No birthday reminder selected"
        }
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(timestamp)
    }
}
