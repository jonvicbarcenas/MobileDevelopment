package com.anime.aniwatch.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.anime.aniwatch.BuildConfig
import com.anime.aniwatch.databinding.SplashBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: SplashBinding
    private val versionUrl = "https://jonvicbarcenas.github.io/MobileDevelopment/version.json"
    private var latestVersion: String? = null
    private var featuresList: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkForUpdate()
        clearUserPref()
    }

    private fun checkForUpdate() {
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            Toast.makeText(this, "Request timed out. Proceeding to the next screen.", Toast.LENGTH_SHORT).show()
            proceedToNextScreen()
        }

        handler.postDelayed(timeoutRunnable, 5000) // Set a 5-second timeout

        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(versionUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody ?: "")
                    latestVersion = jsonObject.getString("version").removePrefix("v")
                    
                    // Parse features array if available
                    if (jsonObject.has("features")) {
                        val featuresArray = jsonObject.getJSONArray("features")
                        val features = mutableListOf<String>()
                        for (i in 0 until featuresArray.length()) {
                            features.add(featuresArray.getString(i))
                        }
                        featuresList = features
                    }

                    runOnUiThread {
                        handler.removeCallbacks(timeoutRunnable)
                        if (latestVersion != BuildConfig.VERSION_NAME) {
                            showUpdatePrompt()
                            Toast.makeText(
                                this,
                                "Update available: $latestVersion" +
                                        "\nCurrent version: ${BuildConfig.VERSION_NAME}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            proceedToNextScreen()
                        }
                    }
                } else {
                    runOnUiThread {
                        handler.removeCallbacks(timeoutRunnable) // Cancel the timeout
                        proceedToNextScreen()
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    handler.removeCallbacks(timeoutRunnable) // Cancel the timeout
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                    proceedToNextScreen()
                }
            }
        }.start()
    }
    private fun showUpdatePrompt() {
        // Build features text
        val featuresText = if (featuresList.isNotEmpty()) {
            val sb = StringBuilder("\n\nNew Features:\n")
            featuresList.forEachIndexed { index, feature ->
                sb.append("${index + 1}. $feature\n")
            }
            sb.toString()
        } else {
            ""
        }
        
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version of the app is available. Please update to continue.$featuresText")
            .setPositiveButton("Update") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://github.com/jonvicbarcenas/MobileDevelopment/releases/download/v$latestVersion/app-release.apk")
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel") { _, _ ->
                proceedToNextScreen()
            }
            .setCancelable(false)
            .show()
    }

    private fun proceedToNextScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun clearUserPref() {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("username")
            .remove("email")
            .remove("profileImageRes")
            .apply()
    }
}