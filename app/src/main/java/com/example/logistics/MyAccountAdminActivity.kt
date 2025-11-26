package com.example.logistics

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyAccountAdminActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvRole: TextView
    private lateinit var totalAddedText: TextView
    private lateinit var totalReachedText: TextView
    private lateinit var totalPendingText: TextView
    
    private val mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account_admin)

        tvName = findViewById(R.id.tvName)
        tvRole = findViewById(R.id.tvRole)
        totalAddedText = findViewById(R.id.totalAddedText)
        totalReachedText = findViewById(R.id.totalReachedText)
        totalPendingText = findViewById(R.id.totalPendingText)

        loadUserData()
        loadUserStats()
    }

    private fun loadUserData() {
        val user = mAuth.currentUser
        val uid = user?.uid 
        
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    tvName.text = document.getString("name") ?: "N/A"
                    tvRole.text = document.getString("role") ?: "User"
                } else {
                    Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading data: ${it.message}", Toast.LENGTH_SHORT).show()
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
                    val status = doc.getString("status")?.lowercase()?.trim()
                    if (status == "pending") {
                        totalPending++
                    } else if (status == "1" || status == "2" || status == "3") {
                        // Assuming these statuses mean reached/completed
                        totalReached++
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
