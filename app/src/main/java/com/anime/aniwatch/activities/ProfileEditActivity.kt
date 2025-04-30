package com.anime.aniwatch.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.anime.aniwatch.R
import com.anime.aniwatch.adapter.ProfileImageAdapter
import com.anime.aniwatch.data.MovieData
import com.anime.aniwatch.databinding.ActivityProfileEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private val TAG = "PROFILE_EDIT_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar with a back button
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        val userEmail = auth.currentUser?.email
        binding.emailadd.setText(userEmail ?: "")

        val uid = auth.currentUser?.uid
        if (uid != null) {
            databaseReference.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.child("username").getValue(String::class.java)
                    binding.username.setText(user ?: "")

                    if (snapshot.hasChild("profileImageRes")) {
                        val profileImageRes = snapshot.child("profileImageRes").getValue(Int::class.java)
                        if (profileImageRes != null) {
                            binding.profileImage.setImageResource(profileImageRes)
                            selectedImageResId = profileImageRes
                        } else {
                            loadProfileImage()
                        }
                    } else {
                        loadProfileImage()
                    }
                } else {
                    loadProfileImage()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                loadProfileImage()
            }
        } else {
            loadProfileImage()
        }

        binding.profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.update.setOnClickListener {
            updateProfile(uid)
        }
    }

    private fun showImagePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.profile_images, null)
        val gridView = dialogView.findViewById<GridView>(R.id.gridViewImages)

        val imageIds = MovieData.avatars.toTypedArray() // Convert to array

        val imageAdapter = ProfileImageAdapter(this, imageIds)
        gridView.adapter = imageAdapter

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Select Profile Image")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedImageRes = imageIds[position]
            setProfileImage(selectedImageRes)
            dialog.dismiss()
        }
    }

    private fun setProfileImage(imageResId: Int) {
        binding.profileImage.setImageResource(imageResId)

        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("profileImageRes", imageResId)
        editor.apply()

        selectedImageResId = imageResId
    }

    private var selectedImageResId: Int? = null

    private fun loadProfileImage() {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val savedImageResId = sharedPreferences.getInt("profileImageRes", R.drawable.account)
        binding.profileImage.setImageResource(savedImageResId)
        selectedImageResId = savedImageResId
    }

    private fun updateProfile(uid: String?) {
        if (uid == null) return

        val username = binding.username.text.toString().trim()
        val email = binding.emailadd.text.toString().trim()

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = hashMapOf<String, Any>(
            "username" to username,
            "email" to email
        )

        if (selectedImageResId != null) {
            userUpdates["profileImageRes"] = selectedImageResId!!
        }

        updateUserDataInDatabase(uid, userUpdates)
    }

    private fun updateUserDataInDatabase(uid: String, userUpdates: HashMap<String, Any>) {
        databaseReference.child(uid).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Profile updated in Firebase: $userUpdates")
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()

                    val resultIntent = Intent()
                    resultIntent.putExtra("updatedUsername", userUpdates["username"] as String)

                    if (userUpdates.containsKey("email")) {
                        resultIntent.putExtra("updatedEmail", userUpdates["email"] as String)
                    }

                    if (userUpdates.containsKey("profileImageRes")) {
                        resultIntent.putExtra("selectedImage", userUpdates["profileImageRes"] as Int)
                    }

                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    Log.e(TAG, "Update failed: ${task.exception?.message}")
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error: ${exception.message}")
                Toast.makeText(this, "Database error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
