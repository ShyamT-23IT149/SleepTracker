package com.example.sleeptracker

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)

        if (preferences.isLoggedIn()) {
            navigateAfterAuth()
            return
        }

        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.etUsername)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupButton = findViewById<Button>(R.id.btnSignup)
        val statusText = findViewById<TextView>(R.id.tvAuthStatus)

        statusText.text = if (preferences.hasRegisteredUser()) {
            "Login with your saved account or create a new one."
        } else {
            "Create your account to start tracking today's sleep."
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (preferences.validateLogin(username, password)) {
                preferences.saveLoginSession(username)
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                navigateAfterAuth()
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (username.isBlank() || password.length < 4) {
                Toast.makeText(this, "Use a username and 4+ character password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Creating account")
            progressDialog.setMessage("Preparing your sleep profile...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            preferences.saveCredentials(username, password)
            preferences.saveLoginSession(username)
            progressDialog.dismiss()
            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ProfileSetupActivity::class.java))
            finish()
        }
    }

    private fun navigateAfterAuth() {
        val destination = if (preferences.hasCompletedProfile()) {
            HomeActivity::class.java
        } else {
            ProfileSetupActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}
