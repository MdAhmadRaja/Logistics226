package com.example.logistics

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchNotifications: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)

        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("DARK_MODE", false)
        val areNotificationsEnabled = sharedPrefs.getBoolean("NOTIFICATIONS", true)

        switchDarkMode.isChecked = isDarkMode
        switchNotifications.isChecked = areNotificationsEnabled

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPrefs.edit()
            editor.putBoolean("DARK_MODE", isChecked)
            editor.apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            recreate()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPrefs.edit()
            editor.putBoolean("NOTIFICATIONS", isChecked)
            editor.apply()
            val msg = if (isChecked) "Notifications Enabled" else "Notifications Disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
