package com.example.sleeptracker

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TeamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team)

        val type = intent.getStringExtra("type") ?: "About Us"
        val content = TeamDatabaseHelper(this).getContentByTitle(type)

        findViewById<TextView>(R.id.tvTeamTitle).text = content?.title ?: type
        findViewById<TextView>(R.id.tvTeamDesc).text = content?.description ?: "No details available."
        findViewById<ImageView>(R.id.imgTeam).setImageResource(resolveImageResource(content?.imageName))
    }

    private fun resolveImageResource(imageName: String?): Int {
        return when (imageName) {
            "member1" -> R.drawable.member1
            "app_logo" -> R.drawable.app_logo
            else -> R.drawable.app_logo
        }
    }
}
