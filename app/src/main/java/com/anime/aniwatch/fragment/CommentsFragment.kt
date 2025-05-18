package com.anime.aniwatch.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anime.aniwatch.R
import com.anime.aniwatch.adapter.CommentsAdapter
import com.anime.aniwatch.data.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class CommentsFragment : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var sendCommentButton: ImageButton
    private lateinit var generateCommentButton: ImageButton
    private lateinit var commentsAdapter: CommentsAdapter
    private var episodeId: String? = null
    private var animeId: String? = null
    private lateinit var commentsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private var commentsListener: ValueEventListener? = null
    private var episodeTitle: String = ""
    private var currentUsername: String = "Anonymous User"
    private var currentProfileImageUrl: String = ""
    private var currentUserUid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comments, container, false)

        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentInput = view.findViewById(R.id.commentInput)
        sendCommentButton = view.findViewById(R.id.sendCommentButton)
        generateCommentButton = view.findViewById(R.id.generateCommentButton)

        episodeId = arguments?.getString("EPISODE_ID")
        animeId = arguments?.getString("ANIME_ID")
        episodeTitle = arguments?.getString("EPISODE_TITLE") ?: ""

        setupRecyclerView()
        setupCommentInput()
        setupGenerateCommentButton()
        setupFirebase()
        fetchCurrentUserInfo()

        return view
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter()
        commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                // Show newest comments first
                reverseLayout = true
                stackFromEnd = true
            }
            adapter = commentsAdapter
        }
    }

    private fun setupCommentInput() {
        sendCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty() && episodeId != null && animeId != null) {
                addComment(commentText)
                commentInput.text.clear()
            } else if (episodeId == null || animeId == null) {
                Toast.makeText(context, "Error: Missing episode information", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGenerateCommentButton() {
        generateCommentButton.setOnClickListener {
            val generatedComment = generateCreativeComment(episodeTitle)
            commentInput.setText(generatedComment)
        }
    }

    private fun setupFirebase() {
        if (episodeId == null) return

        // Initialize Firebase Database reference
        val database = FirebaseDatabase.getInstance()
        commentsRef = database.getReference("comments").child(episodeId!!)
        usersRef = database.getReference("Users")

        // Load comments
        loadComments()
    }

    private fun fetchCurrentUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserUid = currentUser.uid
            
            usersRef.child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Safely get username as String
                        val usernameValue = snapshot.child("username").value
                        if (usernameValue != null) {
                            currentUsername = usernameValue.toString()
                        }
                        
                        // Safely get profileImageRes
                        val profileImageValue = snapshot.child("profileImageRes").value
                        if (profileImageValue != null) {
                            currentProfileImageUrl = profileImageValue.toString()
                        }
                    } catch (e: Exception) {
                        println("Error parsing user data: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Keep default values if there's an error
                }
            })
        } else {
            // Try to get the last used username from the device
            val deviceId = android.provider.Settings.Secure.getString(
                requireContext().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            usersRef.orderByChild("deviceId").equalTo(deviceId).limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnapshot in snapshot.children) {
                                try {
                                    currentUserUid = userSnapshot.key ?: ""
                                    
                                    // Safely get username as String
                                    val usernameValue = userSnapshot.child("username").value
                                    if (usernameValue != null) {
                                        currentUsername = usernameValue.toString()
                                    }
                                    
                                    // Safely get profileImageRes
                                    val profileImageValue = userSnapshot.child("profileImageRes").value
                                    if (profileImageValue != null) {
                                        currentProfileImageUrl = profileImageValue.toString()
                                    }
                                } catch (e: Exception) {
                                    println("Error parsing user data by device: ${e.message}")
                                }
                                break
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Keep default values if there's an error
                    }
                })
        }
    }

    private fun loadComments() {
        commentsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = mutableListOf<Comment>()
                for (commentSnapshot in snapshot.children) {
                    try {
                        val comment = commentSnapshot.getValue(Comment::class.java)
                        comment?.let { comments.add(it) }
                    } catch (e: Exception) {
                        println("Error parsing comment: ${e.message}")
                    }
                }
                
                // Sort by timestamp (newest first)
                comments.sortByDescending { it.timestamp }
                commentsAdapter.submitList(comments)
                
                if (comments.isNotEmpty()) {
                    commentsRecyclerView.scrollToPosition(0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load comments: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        commentsRef.addValueEventListener(commentsListener!!)
    }

    private fun addComment(text: String) {
        val commentId = commentsRef.push().key ?: return
        val currentTime = System.currentTimeMillis()
        
        val newComment = Comment(
            id = commentId,
            username = currentUsername,
            text = text,
            timestamp = currentTime,
            episodeId = episodeId ?: "",
            animeId = animeId ?: "",
            profileImageUrl = currentProfileImageUrl
        )
        
        commentsRef.child(commentId).setValue(newComment)
            .addOnSuccessListener {
                // Comment added successfully
                // No need to update the adapter manually as the ValueEventListener will handle it
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add comment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the Firebase listener to prevent memory leaks
        commentsListener?.let { commentsRef.removeEventListener(it) }
    }

    // Generate creative comments for anime episodes
    fun generateCreativeComment(episodeTitle: String): String {
        val comments = listOf(
            "This episode blew my mind! The animation was incredible!",
            "I can't believe what just happened! The plot twist was unexpected!",
            "The character development in this episode was phenomenal!",
            "The fight scenes were so well choreographed, I had to rewatch them!",
            "I'm emotionally invested in these characters more than ever!",
            "The soundtrack during the climax scene gave me goosebumps!",
            "This episode made me both laugh and cry. What a rollercoaster!",
            "The villain's backstory finally makes sense. Great writing!",
            "I didn't expect that plot twist! Can't wait for the next episode!",
            "The animation quality in this episode was next level!",
            "That ending left me speechless... I need the next episode now!",
            "The voice acting in the emotional scenes was perfect!",
            "This might be my favorite episode of the entire series!",
            "I'm loving the pacing of this story arc!",
            "The new opening theme fits the current arc perfectly!"
        )
        return comments.random()
    }
} 