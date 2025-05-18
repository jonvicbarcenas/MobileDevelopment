package com.anime.aniwatch.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.anime.aniwatch.*
import com.anime.aniwatch.databinding.ActivityMainBinding
import com.anime.aniwatch.fragment.AccountFragment
import com.anime.aniwatch.fragment.HomeFragment
import com.anime.aniwatch.fragment.ListFragment
import com.google.firebase.FirebaseApp
import android.Manifest
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isFragmentTransactionInProgress = false
    private val fragmentTransactionDebounceTime = 300L
    private val USER_PREFS = "userPrefs"
    private val USERNAME_KEY = "username"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showDisclaimerDialog()
        checkUsername()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.title = null

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val fragment = when (menuItem.itemId) {
                R.id.home -> HomeFragment()
                R.id.list -> ListFragment()
                R.id.account -> AccountFragment()
                else -> null
            }
            fragment?.let { replaceFragment(it) }
            fragment != null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    private fun checkUsername() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        // First check local storage
        val sharedPreferences = getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        val localUsername = sharedPreferences.getString(USERNAME_KEY, null)
        
        if (!localUsername.isNullOrEmpty()) {
            // Username exists locally, no need to show dialog
            return
        }
        
        // If no local username, check Firebase
        if (currentUser != null) {
            val uid = currentUser.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users")
            
            databaseReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChild("username")) {
                        val username = snapshot.child("username").getValue(String::class.java)
                        if (!username.isNullOrEmpty()) {
                            // Save username locally
                            sharedPreferences.edit().putString(USERNAME_KEY, username).apply()
                            return
                        }
                    }
                    
                    // No username found, show dialog
                    showUsernameDialog()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // On error, show dialog to be safe
                    showUsernameDialog()
                }
            })
        } else {
            // User not logged in, show dialog
            showUsernameDialog()
        }
    }
    
    private fun showUsernameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_username, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        val usernameInput = dialogView.findViewById<EditText>(R.id.username_input)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirm_button)
        
        confirmButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            
            if (username.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveUsername(username)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun saveUsername(username: String) {
        // Save locally
        val sharedPreferences = getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(USERNAME_KEY, username).apply()
        
        // Save to Firebase if user is logged in
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            val uid = currentUser.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("Users")
            
            databaseReference.child(uid).child("username").setValue(username)
                .addOnSuccessListener {
                    Toast.makeText(this, "Username saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save username online, but saved locally", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        if (isFragmentTransactionInProgress) {
            return
        }

        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.frame_layout)

        if (currentFragment != null && currentFragment::class == fragment::class) {
            return
        }

        isFragmentTransactionInProgress = true

        val fragmentTransaction = fragmentManager.beginTransaction()

        if (fragment is HomeFragment) {
            fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commitAllowingStateLoss()

        // Update UI based on the fragment type
        when (fragment) {
            is HomeFragment -> {
                supportActionBar?.show()
                disableBackButton()
                supportActionBar?.setDisplayShowTitleEnabled(false)
                showSearchButton()
            }
            is ListFragment -> {
                supportActionBar?.show()
                supportActionBar?.setDisplayShowTitleEnabled(true)
                supportActionBar?.title = "My List"
                disableBackButton()
            }
            is AccountFragment -> {
                supportActionBar?.hide()
            }
        }

        binding.root.postDelayed({
            isFragmentTransactionInProgress = false
        }, fragmentTransactionDebounceTime)
    }
    private fun enableBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    private fun disableBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun showSearchButton() {
        val searchButton = binding.toolbar.menu.findItem(R.id.action_search)
        searchButton?.isVisible = true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.action, menu)
        return true
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
        if (currentFragment is HomeFragment) {
            binding.bottomNavigationView.selectedItemId = R.id.home
            finish()
        } else {
            super.onBackPressed()
            val newFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
            if (newFragment is HomeFragment) {
                binding.bottomNavigationView.selectedItemId = R.id.home
            } else if (newFragment is ListFragment) {
                binding.bottomNavigationView.selectedItemId = R.id.list
            } else if (newFragment is AccountFragment) {
                binding.bottomNavigationView.selectedItemId = R.id.account
            }
        }
    }


    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDisclaimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_disclaimer, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(dialogView)
        dialog.setCancelable(false)

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val message = dialogView.findViewById<TextView>(R.id.dialog_message)
        val button = dialogView.findViewById<Button>(R.id.dialog_button)

        title.text = "⚠️ Disclaimer"
        message.text = "AniWatch is developed solely for educational purposes and is intended to serve as a learning tool for exploring concepts such as API communication, integration, and mobile application development. The app demonstrates how to interact with APIs, manage data, and implement various Android features like RecyclerView, custom adapters, and user interface components.\n\n" +
                "We emphasize that AniWatch does not host, stream, or distribute any copyrighted content. The app does not provide access to any illegal or unauthorized sources of anime or other media. All content displayed within the app is either user-provided or simulated for demonstration purposes.\n\n" +
                "By using AniWatch, users agree to take full responsibility for ensuring compliance with copyright laws and regulations in their respective regions. The developers of AniWatch do not condone or support piracy in any form. This project is strictly for educational exploration and should not be used for any activities that violate intellectual property rights."

        button.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
