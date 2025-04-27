package com.anime.aniwatch.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.GridView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.anime.aniwatch.R
import com.anime.aniwatch.adapter.ProfileImageAdapter
import com.anime.aniwatch.data.MovieData
import com.anime.aniwatch.databinding.ActivityProfileEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null
    private val TAG = "PROFILE_EDIT_TAG"

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null && data.data != null) {
                    imageUri = data.data
                    binding.profileImage.setImageURI(imageUri) // Show selected image
                }
            }
        }

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
        storageReference = FirebaseStorage.getInstance().reference

        val userEmail = auth.currentUser?.email
        binding.emailadd.setText(userEmail ?: "")

        val uid = auth.currentUser?.uid
        if (uid != null) {
            databaseReference.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.child("username").getValue(String::class.java)
                    binding.username.setText(user ?: "")
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle profile image click - show predefined images to select
        binding.profileImage.setOnClickListener {
            // Show predefined profile images from drawable
            showImagePickerDialog()
        }

        binding.update.setOnClickListener {
            updateProfile(uid)
        }
    }

    // Show a dialog to pick a profile image
    private fun showImagePickerDialog() {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.profile_images, null)
        val gridView = dialogView.findViewById<GridView>(R.id.gridViewImages)

        // Use MovieData.movieImages for image IDs
        val imageIds = MovieData.avatars.toTypedArray() // Convert to array

        // Initialize the adapter and set it to the GridView
        val imageAdapter = ProfileImageAdapter(this, imageIds)
        gridView.adapter = imageAdapter

        // Create the dialog
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Select Profile Image")
            .setView(dialogView)
            .setCancelable(true)  // Allow dialog to be canceled by tapping outside
            .create()

        // Show the dialog
        dialog.show()

        // Handle the image selection
        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedImageRes = imageIds[position]
            setProfileImage(selectedImageRes)
            dialog.dismiss()
        }
    }


    private fun setProfileImage(imageResId: Int) {
        // Set the selected image to the profile image view
        binding.profileImage.setImageResource(imageResId)

        // Save the selected image to SharedPreferences
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("profileImageRes", imageResId)
        editor.apply()  // Save the changes
    }

    private fun loadProfileImage() {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val savedImageResId = sharedPreferences.getInt("profileImageRes", R.drawable.account)  // Default image if not set
        binding.profileImage.setImageResource(savedImageResId)
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

        // If an image was selected, upload it to Firebase Storage
        if (imageUri != null) {
            val imageRef = storageReference.child("profile_images/${uid}.jpg")

            imageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Store the image URL in the database
                        userUpdates["profileImageUrl"] = uri.toString()

                        // Update user profile with new data
                        updateUserDataInDatabase(uid, userUpdates)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to upload image: ${it.message}")
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    updateUserDataInDatabase(uid, userUpdates) // Update without image if upload fails
                }
        } else {
            // If no image was selected, update the user data without image
            updateUserDataInDatabase(uid, userUpdates)
        }
    }

    private fun updateUserDataInDatabase(uid: String, userUpdates: HashMap<String, Any>) {
        databaseReference.child(uid).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Profile updated in Firebase: $userUpdates")
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()

                    // Send updated username back to AccountFragment
                    val resultIntent = Intent()
                    resultIntent.putExtra("updatedUsername", userUpdates["username"] as String)
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

