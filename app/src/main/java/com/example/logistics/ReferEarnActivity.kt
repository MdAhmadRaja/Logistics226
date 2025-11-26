package com.example.logistics

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReferEarnActivity : AppCompatActivity() {

    private lateinit var btnShare: Button
    private lateinit var tvReferralCode: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refer_earn)

        btnShare = findViewById(R.id.btnShare)
        tvReferralCode = findViewById(R.id.tvReferralCode)

        btnShare.setOnClickListener {
            val code = tvReferralCode.text.toString()
            val message = "Join Logistics App today! Use my referral code $code to earn rewards. Download now: https://play.google.com/store/apps/details?id=com.example.logistics"
            
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, message)
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }
}
