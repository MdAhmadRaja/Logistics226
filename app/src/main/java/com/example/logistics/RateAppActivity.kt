package com.example.logistics

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RateAppActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var btnSubmitRating: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_app)

        ratingBar = findViewById(R.id.ratingBar)
        btnSubmitRating = findViewById(R.id.btnSubmitRating)

        checkIfAlreadyRated()

        btnSubmitRating.setOnClickListener {
            val rating = ratingBar.rating
            if (rating > 0) {
                submitRating(rating)
            } else {
                Toast.makeText(this, "Please select a rating first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIfAlreadyRated() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.contains("myRating")) {
                    val rating = document.getDouble("myRating")?.toFloat() ?: 0f
                    ratingBar.rating = rating
                    ratingBar.isEnabled = false
                    btnSubmitRating.isEnabled = false
                    btnSubmitRating.text = "Already Rated"
                }
            }
    }

    private fun submitRating(rating: Float) {
        val uid = auth.currentUser?.uid ?: return
        
        val updateData = mapOf("myRating" to rating)
        
        db.collection("users").document(uid)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Thank you for rating us $rating stars!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show()
            }
    }
}
