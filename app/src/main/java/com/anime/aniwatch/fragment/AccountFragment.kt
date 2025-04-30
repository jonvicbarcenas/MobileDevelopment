package com.anime.aniwatch.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.anime.aniwatch.activities.ProfileEditActivity
import com.anime.aniwatch.activities.SecurityActivity
import com.anime.aniwatch.activities.SplashActivity
import com.anime.aniwatch.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.auth.UserProfileChangeRequest
import com.anime.aniwatch.R
import com.anime.aniwatch.activities.NotificationsActivity
import com.anime.aniwatch.activities.SettingsActivity

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private lateinit var userData: UserProfileData

    private val USER_PREFS = "userPrefs"
    private val USERNAME_KEY = "username"
    private val EMAIL_KEY = "email"
    private val PROFILE_IMAGE_KEY = "profileImageRes"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        userData = loadUserDataFromLocal()

        val userEmail = userData.email ?: auth.currentUser?.email
        binding.email.text = userEmail ?: "No Email"

        if (!userData.username.isNullOrEmpty()) {
            binding.fullName.text = userData.username
            Log.d("AccountFragment", "Using locally stored username: ${userData.username}")
        } else {
            binding.fullName.text = "Anonymous User"
        }

        binding.profile.setImageResource(userData.profileImageResId)
        Log.d("AccountFragment", "Using locally stored profile image resource: ${userData.profileImageResId}")

        if (uid != null) {
            databaseReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) {
                        Log.d("AccountFragment", "Fragment not attached, skipping data update")
                        return
                    }

                    if (snapshot.exists()) {
                        val username = snapshot.child("username").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)

                        var profileImageResId: Int? = null
                        if (snapshot.hasChild("profileImageRes")) {
                            profileImageResId = snapshot.child("profileImageRes").getValue(Int::class.java)
                        }

                        Log.d("AccountFragment", "Fetched from Firebase: username=$username, email=$email, profileImageResId=$profileImageResId")

                        saveUserDataLocally(username, email, profileImageResId)

                        if (_binding != null) {
                            // Update username if changed
                            if (username != userData.username) {
                                binding.fullName.text = username ?: "Unknown User"
                            }

                            if (email != userData.email && !email.isNullOrEmpty()) {
                                binding.email.text = email
                            }

                            if (profileImageResId != null && profileImageResId != userData.profileImageResId) {
                                binding.profile.setImageResource(profileImageResId)
                                Log.d("AccountFragment", "Updated profile with image resource: $profileImageResId")
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (isAdded) {
                        if (userData.username.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        binding.editProfile.setOnClickListener {
            val intent = Intent(requireContext(), ProfileEditActivity::class.java)
            startActivityForResult(intent, 100)
        }

        binding.notifications.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivityForResult(intent, 100)
        }

        binding.settings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivityForResult(intent, 100)
        }

        binding.security.setOnClickListener {
            val intent = Intent(requireContext(), SecurityActivity::class.java)
            startActivity(intent)
        }

        binding.logout.setOnClickListener {
            showLogoutDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == 100) {
            val updatedUsername = data?.getStringExtra("updatedUsername")
            val updatedEmail = data?.getStringExtra("updatedEmail")

            if (!updatedUsername.isNullOrEmpty()) {
                binding.fullName.text = updatedUsername
            }

            val imageResId = data?.getIntExtra("selectedImage", R.drawable.account) ?: R.drawable.account
            binding.profile.setImageResource(imageResId)
            updateUserProfile(updatedUsername, updatedEmail, imageResId)
        }
    }

    private fun updateUserProfile(updatedUsername: String?, updatedEmail: String?, profileImageResId: Int? = null) {
        saveUserDataLocally(updatedUsername, updatedEmail, profileImageResId)
        Log.d("AccountFragment", "Profile data saved locally first")

        val user = auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            val userUpdates = mutableMapOf<String, Any>(
                "username" to updatedUsername.orEmpty(),
                "email" to updatedEmail.orEmpty()
            )

            if (profileImageResId != null) {
                userUpdates["profileImageRes"] = profileImageResId
            }

            databaseReference.child(uid).updateChildren(userUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    Log.d("AccountFragment", "Profile updated in Firebase Database")
                } else {
                    Toast.makeText(requireContext(), "Failed to update profile online, but saved locally", Toast.LENGTH_SHORT).show()
                    Log.e("AccountFragment", "Failed to update profile in Firebase: ${task.exception?.message}")
                }
            }
        }
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(updatedUsername)
            .setPhotoUri(null)
            .build()

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AccountFragment", "User profile updated in Firebase Auth.")
            } else {
                Log.e("AccountFragment", "Failed to update profile in Firebase Auth: ${task.exception?.message}")
            }
        }

        updatedEmail?.let {
            user?.updateEmail(it)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AccountFragment", "Email updated in Firebase Auth.")
                } else {
                    Toast.makeText(requireContext(), "Failed to update email online, but saved locally", Toast.LENGTH_SHORT).show()
                    Log.e("AccountFragment", "Failed to update email in Firebase Auth: ${task.exception?.message}")
                }
            }
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        btnConfirm.setOnClickListener {
            logoutUser()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun logoutUser() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("email", "") ?: ""
        val savedPassword = sharedPreferences.getString("password", "") ?: ""
        val isRememberMe = sharedPreferences.getBoolean("isLoggedIn", false)

        sharedPreferences.edit().apply {
            clear()
            putString("email", savedEmail)
            putString("password", savedPassword)
            putBoolean("isLoggedIn", isRememberMe)
            apply()
        }

        auth.signOut()

        val intent = Intent(activity, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveUserDataLocally(username: String?, email: String?, profileImageResId: Int? = null) {
        if (!isAdded) {
            Log.d("AccountFragment", "Cannot save user data locally - fragment not attached to context")
            return
        }

        try {
            val sharedPreferences = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                username?.let { putString(USERNAME_KEY, it) }
                email?.let { putString(EMAIL_KEY, it) }
                profileImageResId?.let { putInt(PROFILE_IMAGE_KEY, it) }
                apply()
            }
            Log.d("AccountFragment", "User data saved locally: username=$username, email=$email, profileImageResId=$profileImageResId")
        } catch (e: IllegalStateException) {
            Log.e("AccountFragment", "Failed to save user data locally: ${e.message}")
        }
    }

    private fun loadUserDataFromLocal(): UserProfileData {
        val sharedPreferences = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        val username = sharedPreferences.getString(USERNAME_KEY, null)
        val email = sharedPreferences.getString(EMAIL_KEY, null)
        val profileImageResId = sharedPreferences.getInt(PROFILE_IMAGE_KEY, R.drawable.account)

        Log.d("AccountFragment", "User data loaded from local: username=$username, email=$email, profileImageResId=$profileImageResId")

        return UserProfileData(username, email, profileImageResId)
    }

    data class UserProfileData(
        val username: String?,
        val email: String?,
        val profileImageResId: Int
    )
}