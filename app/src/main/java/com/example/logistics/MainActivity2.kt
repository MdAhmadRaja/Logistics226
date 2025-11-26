package com.example.logistics

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val dashboardIcon = findViewById<ImageView>(R.id.dashboard_icon)

        // Dashboard icon click listener
        dashboardIcon.setOnClickListener {
            val intent = Intent(this, Graph::class.java)
            startActivity(intent)
        }

        // Default fragment
        loadFragment(HomeFragment())
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
