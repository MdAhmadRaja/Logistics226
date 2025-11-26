package com.example.logistics

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyAccountActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var btnLogout: Button
    private lateinit var ivProfileImage: ImageView
    private val mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvRole = findViewById(R.id.tvRole)
        btnLogout = findViewById(R.id.btnLogout)
        ivProfileImage = findViewById(R.id.ivProfileImage)

        loadUserData()

        btnLogout.setOnClickListener {
            mAuth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
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
                    
                    // Handle email logic: prefer auth email, then firestore email, else N/A
                    val email = user.email ?: document.getString("email") ?: "N/A"
                    tvEmail.text = email
                    
                    tvRole.text = document.getString("role") ?: "User"
                    
                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(profileImageUrl).circleCrop().into(ivProfileImage)
                        ivProfileImage.setPadding(0,0,0,0) // Remove padding if image loaded
                    }
                } else {
                    Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
