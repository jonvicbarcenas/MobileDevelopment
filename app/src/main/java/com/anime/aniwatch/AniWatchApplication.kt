package com.anime.aniwatch

import android.app.Application
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.anime.aniwatch.data.AppVersionInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AniWatchApplication : Application() {
    
    companion object {
        private const val TAG = "AniWatchApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            
            saveAppVersionInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application: ${e.message}", e)
        }
    }

    private fun saveAppVersionInfo() {
        try {
            val database = FirebaseDatabase.getInstance()
            val auth = FirebaseAuth.getInstance()
            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            // Get device ID (for anonymous tracking)
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: 
                           UUID.randomUUID().toString()
            
            val appVersionInfo = AppVersionInfo(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                lastUsed = currentDate,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.RELEASE
            )
            
            // Save for logged-in user if available
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Save directly under Users node
                database.getReference("Users").child(userId).child("appInfo").setValue(appVersionInfo)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving user app version info: ${e.message}", e)
                    }
                    .addOnSuccessListener {
                        Log.d(TAG, "App version info saved successfully under Users/$userId")
                    }
            } else {
                // For anonymous users, save under a separate node
                database.getReference("AnonymousUsers").child(deviceId).setValue(appVersionInfo)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving anonymous user app info: ${e.message}", e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving app version info: ${e.message}", e)
        }
    }
} 