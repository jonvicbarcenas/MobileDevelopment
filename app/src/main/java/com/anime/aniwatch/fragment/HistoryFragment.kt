package com.anime.aniwatch.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anime.aniwatch.R
import com.anime.aniwatch.data.WatchHistory
import com.anime.aniwatch.adapter.HistoryAdapter
import com.anime.aniwatch.data.UserStreak
import com.anime.aniwatch.network.AnimeResponse
import com.anime.aniwatch.network.ApiService
import com.anime.aniwatch.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var database: DatabaseReference
    private lateinit var streakDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var emptyHistoryText: TextView
    private val watchHistoryList = mutableListOf<WatchHistory>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = HistoryAdapter(watchHistoryList)
        recyclerView.adapter = historyAdapter

        emptyHistoryText = view.findViewById(R.id.emptyHistoryText)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("WatchHistory")
        streakDatabase = FirebaseDatabase.getInstance().getReference("UserStreaks")

        fetchWatchHistory()
        updateUserStreak()

        return view
    }

    private fun fetchAnimePoster(animeId: String, callback: (String?) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        apiService.getAnimeDetails(animeId).enqueue(object : Callback<AnimeResponse> {
            override fun onResponse(call: Call<AnimeResponse>, response: Response<AnimeResponse>) {
                if (response.isSuccessful) {
                    val posterUrl = response.body()?.data?.anime?.info?.poster
                    callback(posterUrl)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<AnimeResponse>, t: Throwable) {
                callback(null)
            }
        })
    }

    private fun updateUserStreak() {
        val userId = auth.currentUser?.uid ?: return
        val currentDate = UserStreak.getCurrentDateString()
        
        // First, check if there are any watch history entries for today
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasWatchedToday = false
                var mostRecentWatchDate = ""
                
                // Find the most recent watch history entry
                for (historySnapshot in snapshot.children) {
                    val watchHistory = historySnapshot.getValue(WatchHistory::class.java)
                    if (watchHistory != null && watchHistory.dateWatched.isNotEmpty()) {
                        val watchDate = UserStreak.extractDateFromTimestamp(watchHistory.dateWatched)
                        
                        // Check if watched today
                        if (watchDate == currentDate) {
                            hasWatchedToday = true
                        }
                        
                        // Keep track of most recent watch date
                        if (mostRecentWatchDate.isEmpty() || watchHistory.dateWatched > mostRecentWatchDate) {
                            mostRecentWatchDate = watchHistory.dateWatched
                        }
                    }
                }
                
                // Only update streak if user has watched something today
                if (hasWatchedToday) {
                    updateStreakData(userId, currentDate)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to check watch history", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateStreakData(userId: String, currentDate: String) {
        streakDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userStreak = snapshot.getValue(UserStreak::class.java) ?: UserStreak()
                
                if (userStreak.lastWatchDate.isEmpty()) {
                    // First time user is watching something
                    userStreak.currentStreak = 1
                    userStreak.longestStreak = 1
                    userStreak.lastWatchDate = currentDate
                    userStreak.streakDates.add(currentDate)
                } else if (UserStreak.isConsecutiveDay(userStreak.lastWatchDate, currentDate)) {
                    // Check if we already counted today
                    val lastDateClean = UserStreak.extractDateFromTimestamp(userStreak.lastWatchDate)
                    if (lastDateClean != currentDate) {
                        userStreak.currentStreak++
                        userStreak.lastWatchDate = currentDate
                        userStreak.streakDates.add(currentDate)
                        
                        // Update longest streak if current is longer
                        if (userStreak.currentStreak > userStreak.longestStreak) {
                            userStreak.longestStreak = userStreak.currentStreak
                        }
                    }
                } else if (UserStreak.extractDateFromTimestamp(userStreak.lastWatchDate) != currentDate) {
                    // Streak broken, reset to 1
                    userStreak.currentStreak = 1
                    userStreak.lastWatchDate = currentDate
                    userStreak.streakDates.clear()
                    userStreak.streakDates.add(currentDate)
                }
                
                // Save updated streak data
                streakDatabase.child(userId).setValue(userStreak)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to update streak", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchWatchHistory() {
        val userId = auth.currentUser?.uid ?: return
        database.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                watchHistoryList.clear()
                val tempList = mutableListOf<WatchHistory>()

                if (snapshot.childrenCount == 0L) {
                    emptyHistoryText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return
                }

                emptyHistoryText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                for (historySnapshot in snapshot.children) {
                    val watchHistory = historySnapshot.getValue(WatchHistory::class.java)
                    if (watchHistory != null) {
                        fetchAnimePoster(watchHistory.animeId) { posterUrl ->
                            watchHistory.animePosterUrl = posterUrl ?: ""
                            if (watchHistory.episodeNumber > 0 && watchHistory.episodeTitle.isNotEmpty()) {
                                tempList.add(watchHistory)
                            }
                            if (tempList.size == snapshot.childrenCount.toInt()) {
                                tempList.sortByDescending { it.dateWatched }
                                watchHistoryList.addAll(tempList)
                                historyAdapter.notifyDataSetChanged()

                                // Check if after filtering, the list is empty
                                if (watchHistoryList.isEmpty()) {
                                    emptyHistoryText.visibility = View.VISIBLE
                                    recyclerView.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load history", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
