package com.example.sleeptracker

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val sleepHours: Int = 8,
    val friendName: String = "",
    val birthdayReminderAt: Long = 0L
)

class AppPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(username: String, password: String) {
        preferences.edit()
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun hasRegisteredUser(): Boolean = preferences.contains(KEY_USERNAME) && preferences.contains(KEY_PASSWORD)

    fun validateLogin(username: String, password: String): Boolean {
        return username.trim() == preferences.getString(KEY_USERNAME, "") &&
            password == preferences.getString(KEY_PASSWORD, "")
    }

    fun saveLoginSession(username: String) {
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_SESSION_USERNAME, username.trim())
            .apply()
    }

    fun clearLoginSession() {
        preferences.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_SESSION_USERNAME)
            .apply()
    }

    fun isLoggedIn(): Boolean = preferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getSessionUsername(): String = preferences.getString(KEY_SESSION_USERNAME, "") ?: ""

    fun saveUserProfile(profile: UserProfile) {
        preferences.edit()
            .putString(KEY_PROFILE_NAME, profile.name.trim())
            .putString(KEY_PROFILE_USERNAME, profile.username.trim())
            .putString(KEY_PROFILE_EMAIL, profile.email.trim())
            .putString(KEY_PROFILE_PHONE, profile.phone.trim())
            .putInt(KEY_PROFILE_SLEEP_HOURS, profile.sleepHours)
            .putString(KEY_FRIEND_NAME, profile.friendName.trim())
            .putLong(KEY_FRIEND_BIRTHDAY_AT, profile.birthdayReminderAt)
            .apply()
    }

    fun getUserProfile(): UserProfile {
        return UserProfile(
            name = preferences.getString(KEY_PROFILE_NAME, "") ?: "",
            username = preferences.getString(KEY_PROFILE_USERNAME, "") ?: "",
            email = preferences.getString(KEY_PROFILE_EMAIL, "") ?: "",
            phone = preferences.getString(KEY_PROFILE_PHONE, "") ?: "",
            sleepHours = preferences.getInt(KEY_PROFILE_SLEEP_HOURS, 8),
            friendName = preferences.getString(KEY_FRIEND_NAME, "") ?: "",
            birthdayReminderAt = preferences.getLong(KEY_FRIEND_BIRTHDAY_AT, 0L)
        )
    }

    fun hasCompletedProfile(): Boolean = getUserProfile().name.isNotBlank()

    fun saveTodaySleepLog(entry: SleepModel) {
        val currentEntries = getHistoryEntries().toMutableList()
        val todayKey = buildDateKey(System.currentTimeMillis())
        val index = currentEntries.indexOfFirst { it.dayKey == todayKey }
        if (index >= 0) {
            currentEntries[index] = entry
        } else {
            currentEntries.add(0, entry)
        }
        saveHistoryEntries(currentEntries)
    }

    fun getTodaySleepLog(): SleepModel? {
        val todayKey = buildDateKey(System.currentTimeMillis())
        return getHistoryEntries().firstOrNull { it.dayKey == todayKey }
    }

    fun addHistoryEntry(details: String) {
        val timestamp = System.currentTimeMillis()
        val dayKey = buildDateKey(timestamp)
        val log = SleepModel(
            dayKey = "${dayKey}_event_$timestamp",
            dateLabel = formatDate(timestamp),
            bedtime = "--",
            recommendedWakeup = "--",
            sleepHours = 0,
            note = details,
            actualTrackedDuration = ""
        )
        val currentEntries = getHistoryEntries().toMutableList()
        currentEntries.add(0, log)
        saveHistoryEntries(currentEntries)
    }

    fun getHistoryEntries(): List<SleepModel> {
        val rawEntries = preferences.getStringSet(KEY_HISTORY, emptySet()).orEmpty()
        return rawEntries.mapNotNull { rawEntry ->
            runCatching {
                val json = JSONObject(rawEntry)
                SleepModel(
                    dayKey = json.getString("dayKey"),
                    dateLabel = json.getString("dateLabel"),
                    bedtime = json.getString("bedtime"),
                    recommendedWakeup = json.getString("recommendedWakeup"),
                    sleepHours = json.getInt("sleepHours"),
                    note = json.optString("note"),
                    actualTrackedDuration = json.optString("actualTrackedDuration")
                )
            }.getOrNull()
        }.sortedByDescending { it.sortableTime }
    }

    fun clearHistoryEntries() {
        preferences.edit().remove(KEY_HISTORY).apply()
    }

    fun setServiceRunning(startedAt: Long) {
        preferences.edit()
            .putBoolean(KEY_SERVICE_RUNNING, true)
            .putLong(KEY_SERVICE_STARTED_AT, startedAt)
            .apply()
    }

    fun clearServiceRunning() {
        preferences.edit()
            .putBoolean(KEY_SERVICE_RUNNING, false)
            .remove(KEY_SERVICE_STARTED_AT)
            .apply()
    }

    fun isServiceRunning(): Boolean = preferences.getBoolean(KEY_SERVICE_RUNNING, false)

    fun getServiceStartedAt(): Long = preferences.getLong(KEY_SERVICE_STARTED_AT, 0L)

    private fun saveHistoryEntries(entries: List<SleepModel>) {
        val rawEntries = entries.map { entry ->
            JSONObject().apply {
                put("dayKey", entry.dayKey)
                put("dateLabel", entry.dateLabel)
                put("bedtime", entry.bedtime)
                put("recommendedWakeup", entry.recommendedWakeup)
                put("sleepHours", entry.sleepHours)
                put("note", entry.note)
                put("actualTrackedDuration", entry.actualTrackedDuration)
            }.toString()
        }.toSet()
        preferences.edit().putStringSet(KEY_HISTORY, rawEntries).apply()
    }

    private fun buildDateKey(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    companion object {
        private const val PREF_NAME = "sleep_tracker_prefs"
        private const val KEY_USERNAME = "registered_username"
        private const val KEY_PASSWORD = "registered_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SESSION_USERNAME = "session_username"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_USERNAME = "profile_username"
        private const val KEY_PROFILE_EMAIL = "profile_email"
        private const val KEY_PROFILE_PHONE = "profile_phone"
        private const val KEY_PROFILE_SLEEP_HOURS = "profile_sleep_hours"
        private const val KEY_FRIEND_NAME = "friend_name"
        private const val KEY_FRIEND_BIRTHDAY_AT = "friend_birthday_at"
        private const val KEY_HISTORY = "history_entries"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_SERVICE_STARTED_AT = "service_started_at"
    }
}
