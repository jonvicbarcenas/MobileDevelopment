package com.anime.aniwatch.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anime.aniwatch.R
import com.anime.aniwatch.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import java.util.concurrent.TimeUnit

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Initialize Remember Me
        val rememberMe = sharedPrefs.getBoolean("rememberMe", false)
        binding.rememberMeCheckbox.isChecked = rememberMe

        // Session check
        val sessionExpiration = sharedPrefs.getLong("sessionExpiration", 0L)
        val isSessionValid = System.currentTimeMillis() < sessionExpiration

        if (rememberMe) {
            val email = sharedPrefs.getString("email", "") ?: ""
            val password = sharedPrefs.getString("password", "") ?: ""
            binding.signinEmail.setText(email)
            binding.signinPassword.setText(password)

            if (email.isNotEmpty() && password.isNotEmpty() && isSessionValid) {
                autoLogin(email, password)
            }
        }

        binding.rememberMeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("rememberMe", isChecked).apply()
        }

        binding.signinButton.setOnClickListener {
            val email = binding.signinEmail.text.toString()
            val password = binding.signinPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                            val editor = sharedPrefs.edit()
                            editor.putLong("sessionExpiration", expirationTime)

                            if (binding.rememberMeCheckbox.isChecked) {
                                editor.putString("email", email)
                                editor.putString("password", password)
                                editor.putBoolean("rememberMe", true)
                            } else {
                                editor.remove("email")
                                editor.remove("password")
                                editor.putBoolean("rememberMe", false)
                            }

                            editor.apply()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            handleSignInError(task.exception)
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter a valid email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.signupRedirectText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun autoLogin(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val newExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                    sharedPrefs.edit().putLong("sessionExpiration", newExpiration).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    handleSignInError(task.exception)
                }
            }
    }

    private fun handleSignInError(exception: Exception?) {
        if (exception is FirebaseAuthInvalidUserException) {
            val errorCode = exception.errorCode
            if (errorCode == "ERROR_USER_NOT_FOUND") {
                Toast.makeText(this, "Account does not exist or has been deleted.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
        clearSessionData()
    }

    private fun clearSessionData() {
        val editor = sharedPrefs.edit()
        editor.remove("sessionExpiration")
        editor.apply()
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.forgot, null)
        val userEmail = view.findViewById<EditText>(R.id.editBox)
        val btnReset = view.findViewById<Button>(R.id.btnReset)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        builder.setView(view)
        val dialog = builder.create()

        btnReset.setOnClickListener {
            val email = userEmail.text.toString()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Check your email", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Enter a valid email address.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(0))
        dialog.show()
    }
}
