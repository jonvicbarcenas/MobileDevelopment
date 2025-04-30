package com.anime.aniwatch.activities


import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.anime.aniwatch.R

class NotificationsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        notificationSwitch = findViewById(R.id.notif_switch)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)

        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notificationSwitch.isChecked = notificationsEnabled

        notificationSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            try {
                if (isChecked) {
                    enableNotifications() // Enable notifications
                    Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                } else {
                    disableNotifications() // Disable notifications
                    Toast.makeText(this, "Notifications Disabled", Toast.LENGTH_SHORT).show()
                }
                sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            } catch (e: Exception) {
                e.printStackTrace() // Log the exception to the console
                Toast.makeText(this, "Error handling switch action", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun enableNotifications() {
        println("Notifications enabled for scheduled anime")
        Toast.makeText(this, "Notifications enabled for scheduled anime", Toast.LENGTH_SHORT).show()
    }

    private fun disableNotifications() {
        println("Notifications disabled for scheduled anime")
        Toast.makeText(this, "Notifications disabled for scheduled anime", Toast.LENGTH_SHORT).show()
    }
}
