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

        fetchWatchHistory()

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
    }}
