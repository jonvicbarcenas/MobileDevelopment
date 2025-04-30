package com.anime.aniwatch.util

import android.content.Context
import android.widget.Toast
import com.anime.aniwatch.data.WatchlistEpisode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatchlistUtil {

    fun saveToWatchlist(context: Context, watchlistEpisode: WatchlistEpisode) {
        val auth = FirebaseAuth.getInstance()
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val watchlistRef: DatabaseReference = database.getReference("Watchlist").child(userId)

        if (watchlistEpisode.dateAdded.isEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            watchlistEpisode.dateAdded = dateFormat.format(Date())
        }

        val uniqueKey = "${watchlistEpisode.episodeId}"

        watchlistRef.child(uniqueKey).setValue(watchlistEpisode).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Added to watchlist", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add to watchlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeFromWatchlist(context: Context, animeId: String, episodeId: String) {
        val auth = FirebaseAuth.getInstance()
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val watchlistRef: DatabaseReference = database.getReference("Watchlist").child(userId)

        val uniqueKey = "$episodeId"

        watchlistRef.child(uniqueKey).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Removed from watchlist", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to remove from watchlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isInWatchlist(animeId: String, episodeId: String, callback: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            callback(false)
            return
        }

        val uniqueKey = "$episodeId"
        val watchlistRef = database.getReference("Watchlist").child(userId).child(uniqueKey)

        watchlistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }
}
