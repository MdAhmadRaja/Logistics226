package com.example.logistics

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    private lateinit var switchCircle: View
    private lateinit var roleA: LinearLayout
    private lateinit var roleB: LinearLayout
    private lateinit var roleAText: TextView
    private lateinit var roleBText: TextView
    private lateinit var googleAuthBtn: LinearLayout
    private lateinit var rotatingNeon: ImageView

    private var selectedRole = "NONE"
    private var leftX = 0f
    private var rightX = 0f
    private var isDragging = false

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    Toast.makeText(this, "Sign-In failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        switchCircle = findViewById(R.id.switchCircle)
        roleA = findViewById(R.id.roleA)
        roleB = findViewById(R.id.roleB)
        roleAText = findViewById(R.id.roleAText)
        roleBText = findViewById(R.id.roleBText)
        googleAuthBtn = findViewById(R.id.googleAuthBtn)
        rotatingNeon = findViewById(R.id.rotatingNeon)

        // 🔹 Stop background rotation but animate border color
        rotatingNeon.clearAnimation()
        animateBorderColor(rotatingNeon)

        // 🔹 Check if already logged in
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val existingRole = doc.getString("role")
                    existingRole?.let { navigateToRole(it) }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            return
        }

        // 🔹 Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 🔹 Calculate drag boundaries
        switchCircle.post {
            leftX = roleA.x + 10
            rightX = roleB.x + roleB.width - switchCircle.width - 10
        }

        // 🔹 Handle drag gesture
        switchCircle.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val parent = v.parent as View
                        val newX = event.rawX - v.width / 2 - parent.left
                        val minX = leftX
                        val maxX = rightX
                        if (newX in minX..maxX) {
                            v.x = newX
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    handleRoleSelection()
                    true
                }

                else -> false
            }
        }

        // 🔹 Google Sign-in button
        googleAuthBtn.setOnClickListener {
            if (selectedRole == "NONE") {
                Toast.makeText(this, "Please drag to select a role", Toast.LENGTH_SHORT).show()
            } else {
                val intent = googleSignInClient.signInIntent
                signInLauncher.launch(intent)
            }
        }
    }

    // 🧭 Handle drag direction logic
    private fun handleRoleSelection() {
        val middle = (leftX + rightX) / 2
        val currentX = switchCircle.x

        if (currentX < middle) {
            // Move to left → show left text on right
            selectedRole = "owner"
            switchCircle.animate().x(leftX).setDuration(250).start()
            showLeftTextOnRight()
        } else {
            // Move to right → show right text on left
            selectedRole = "user"
            switchCircle.animate().x(rightX).setDuration(250).start()
            showRightTextOnLeft()
        }
    }

    // 🔹 When dragged to left, show left role on right
    private fun showLeftTextOnRight() {
        roleBText.text = "LOGISTICS OWNER"
        roleAText.text = ""
        roleBText.setTextColor(Color.WHITE)
        roleAText.setTextColor(Color.TRANSPARENT)
        roleBText.animate().alpha(1f).setDuration(250).start()
        animateGlow(roleBText)
    }

    // 🔹 When dragged to right, show right role on left
    private fun showRightTextOnLeft() {
        roleAText.text = "VEHICLE OWNER"
        roleBText.text = ""
        roleAText.setTextColor(Color.WHITE)
        roleBText.setTextColor(Color.TRANSPARENT)
        roleAText.animate().alpha(1f).setDuration(250).start()
        animateGlow(roleAText)
    }

    // ✨ Neon glow animation
    private fun animateGlow(targetView: TextView) {
        val scaleUp = ScaleAnimation(
            1f, 1.15f, 1f, 1.15f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 250
            fillAfter = true
        }

        val scaleDown = ScaleAnimation(
            1.15f, 1f, 1.15f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            startOffset = 250
            duration = 250
            fillAfter = true
        }

        targetView.startAnimation(scaleUp)
        targetView.startAnimation(scaleDown)
    }

    // 🌈 Border color flow animation
    private fun animateBorderColor(view: ImageView) {
        val colors = arrayOf(
            Color.parseColor("#00FFAA"),
            Color.parseColor("#00D4FF"),
            Color.parseColor("#8A2BE2"),
            Color.parseColor("#FF00FF")
        )

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), *colors)
        animator.duration = 4000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int
            val bg = view.background as? GradientDrawable
            bg?.setStroke(6, color)
        }
        animator.start()
    }

    // 🔹 Firebase authentication with Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Signing in...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = mAuth.currentUser ?: return@addOnCompleteListener
                val userRef = firestore.collection("users").document(user.uid)

                userRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val existingRole = doc.getString("role")
                        if (existingRole != selectedRole) {
                            Toast.makeText(
                                this,
                                "Already registered as $existingRole",
                                Toast.LENGTH_LONG
                            ).show()
                            mAuth.signOut()
                            googleSignInClient.signOut()
                        } else {
                            navigateToRole(selectedRole)
                        }
                    } else {
                        val data = hashMapOf(
                            "name" to user.displayName,
                            "email" to user.email,
                            "role" to selectedRole
                        )
                        userRef.set(data)
                        navigateToRole(selectedRole)
                    }
                    progressDialog.dismiss()
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToRole(role: String) {
        val intent = when (role) {
            "owner" -> Intent(this, MainActivity2::class.java)
            "user" -> Intent(this, MainActivity::class.java)
            else -> null
        }
        intent?.let {
            startActivity(it)
            finish()
        }
    }
}
