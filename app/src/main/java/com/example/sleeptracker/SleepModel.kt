package com.example.sleeptracker

data class SleepModel(
    val dayKey: String,
    val dateLabel: String,
    val bedtime: String,
    val recommendedWakeup: String,
    val sleepHours: Int,
    val note: String = "",
    val actualTrackedDuration: String = ""
) {
    val sortableTime: Long
        get() = dayKey.filter { it.isDigit() }.toLongOrNull() ?: 0L
}
