package com.example.logistics

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class ProfileFragment : Fragment() {

    private val mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var userInitial: TextView
    private lateinit var userName: TextView
    private lateinit var userPhone: TextView
    private lateinit var logoutBtn: Button
    private lateinit var profileImageContainer: FrameLayout

    private lateinit var btnMyAccount: LinearLayout
    private lateinit var btnReferEarn: LinearLayout
    private lateinit var btnRateApp: LinearLayout
    private lateinit var btnPrivacyPolicy: LinearLayout
    private lateinit var btnTermsCondition: LinearLayout
    private lateinit var btnSettings: LinearLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        userInitial = view.findViewById(R.id.userInitial)
        userName = view.findViewById(R.id.userName)
        userPhone = view.findViewById(R.id.userPhone)
        logoutBtn = view.findViewById(R.id.logoutBtn)
        profileImageContainer = view.findViewById(R.id.profileImageContainer)

        btnMyAccount = view.findViewById(R.id.btnMyAccount)
        btnReferEarn = view.findViewById(R.id.btnReferEarn)
        btnRateApp = view.findViewById(R.id.btnRateApp)
        btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy)
        btnTermsCondition = view.findViewById(R.id.btnTermsCondition)
        btnSettings = view.findViewById(R.id.btnSettings)

        loadUserDetails()

        logoutBtn.setOnClickListener {
            mAuth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        btnMyAccount.setOnClickListener {
            startActivity(/* intent = */ Intent(requireContext(), Dashboard ::class.java))
        }
        btnReferEarn.setOnClickListener {
            startActivity(Intent(requireContext(), ReferEarnActivity::class.java))
        }
        btnRateApp.setOnClickListener {
            startActivity(Intent(requireContext(), RateAppActivity::class.java))
        }
        btnPrivacyPolicy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
        }
        btnTermsCondition.setOnClickListener {
            startActivity(Intent(requireContext(), TermsConditionsActivity::class.java))
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        return view
    }

    private fun loadUserDetails() {
        val uid = mAuth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown User"
                val phone = doc.getString("phone") ?: doc.getString("mobile") ?: "No Number"

                userName.text = name
                userPhone.text = phone

                if (name.isNotEmpty()) {
                    userInitial.text = name.substring(0, 1).uppercase()
                }
            }
            .addOnFailureListener {
                userName.text = "Error Loading"
            }
    }

    private fun showRatingPopup() {
        val dialogView = layoutInflater.inflate(R.layout.rating_popup, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val submitRatingBtn = dialogView.findViewById<Button>(R.id.submitRatingBtn)

        submitRatingBtn.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            Toast.makeText(requireContext(), "Thanks for rating us $rating★!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }
}


