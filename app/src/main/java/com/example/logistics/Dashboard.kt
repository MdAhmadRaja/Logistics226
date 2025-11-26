package com.example.logistics

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var profileImage: ImageView
    private lateinit var nameText: TextView
    private lateinit var roleText: TextView
    private lateinit var logoutBtn: Button
    private lateinit var dashboardLayout: LinearLayout
    private lateinit var totalAddedText: TextView
    private lateinit var totalReachedText: TextView
    private lateinit var totalPendingText: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)



        // Initialize Views
        profileImage = findViewById(R.id.profileImage)
        nameText = findViewById(R.id.nameText)
        roleText = findViewById(R.id.roleText)
        logoutBtn = findViewById(R.id.logoutBtn)
        totalAddedText = findViewById(R.id.totalAddedText)
        totalReachedText = findViewById(R.id.totalReachedText)
        totalPendingText = findViewById(R.id.totalPendingText)
        dashboardLayout = findViewById(R.id.dashboardLayout)

        // Load data
        loadUserProfile()
        loadUserStats()

        // Dashboard click → Graph Activity
        dashboardLayout.setOnClickListener {
            startActivity(Intent(this, Graph::class.java))
        }

        // Logout
        logoutBtn.setOnClickListener {
            mAuth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadUserProfile() {
        val user = mAuth.currentUser ?: return

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nameText.text = doc.getString("name") ?: "N/A"
                    roleText.text = doc.getString("role") ?: "N/A"

                    val imageUrl = doc.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.img_3)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.img_3)
                    }
                }
            }
    }

    private fun loadUserStats() {
        val user = mAuth.currentUser ?: return

        db.collection("logistics")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                var totalAdded = 0
                var totalReached = 0
                var totalPending = 0

                for (doc in snapshot.documents) {
                    totalAdded++
                    when (doc.getString("status")) {
                        "pending" -> totalPending++
                        "1", "2", "3" -> totalReached++
                    }
                }

                totalAddedText.text = totalAdded.toString()
                totalReachedText.text = totalReached.toString()
                totalPendingText.text = totalPending.toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show()
            }
    }
}
