package com.example.logistics

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔹 Apply theme before setting layout
        val sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("DARK_MODE", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ Toolbar icons (match IDs from XML)
        val menuIcon = findViewById<ImageView>(R.id.left_image)
        val profileIcon = findViewById<ImageView>(R.id.right_image)

        // ✅ Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 🔹 Default fragment: Home
        if (savedInstanceState == null) {
            loadFragment(HomeFragmentUser())
        }

//        // 🔹 Handle toolbar icon clicks
//        menuIcon.setOnClickListener {
//            val intent = Intent(this, MapActivity::class.java)
//            startActivity(intent)
//        }

        profileIcon.setOnClickListener {
            loadFragment(ProfileFragmentUser())
        }

        // 🔹 Bottom Navigation fragment switching
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragmentUser())
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragmentUser())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragmentUser())
                    true
                }
                else -> false
            }
        }
    }

    // ✅ Helper function to switch fragments
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
