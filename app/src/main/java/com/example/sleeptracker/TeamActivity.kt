package com.example.sleeptracker

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TeamActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team)

        val title = intent.getStringExtra("type") ?: "Details"
        val tvTitle = findViewById<TextView>(R.id.tvTeamTitle)
        val tvDesc = findViewById<TextView>(R.id.tvTeamDesc)
        val img = findViewById<ImageView>(R.id.imgTeam)

        tvTitle.text = title

        when(title) {
            "About Us" -> {
                tvDesc.text = "Our Sleep Tracker app ensures safety and health monitoring."
                img.setImageResource(R.drawable.app_logo)
            }
            "Team Details" -> {
                tvDesc.text = "Team: Sleep Keepers\nProject: Final Year Implementation"
                img.setImageResource(android.R.drawable.ic_menu_info_details)
            }
            "Team Members" -> {
                tvDesc.text = "Lead Developer: Shyam T"
                // Uses the file you have: member1.jpg
                img.setImageResource(R.drawable.member1)
            }
            "Project Description" -> {
                tvDesc.text = "Features: Foreground Service, SMS Alerts, Location Geocoding."
                // REMOVED project_bg and used a system icon to prevent errors
                img.setImageResource(android.R.drawable.ic_menu_edit)
            }
            else -> {
                tvDesc.text = "Details not found."
                img.setImageResource(R.drawable.app_logo)
            }
        }
    }
}