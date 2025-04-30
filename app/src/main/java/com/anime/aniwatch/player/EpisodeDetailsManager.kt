package com.anime.aniwatch.player

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.anime.aniwatch.network.AnimeResponse
import com.anime.aniwatch.network.ApiService
import com.anime.aniwatch.network.EpisodeResponse
import com.anime.aniwatch.util.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EpisodeDetailsManager(private val context: Context) {
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(ApiService::class.java)
    
    fun fetchEpisodeDetails(
        animeId: String,
        episodeId: String,
        animeTitleText: TextView,
        episodeTitleText: TextView,
        episodeNumberText: TextView
    ) {
        apiService.getAnimeDetails(animeId).enqueue(object : Callback<AnimeResponse> {
            override fun onResponse(call: Call<AnimeResponse>, response: Response<AnimeResponse>) {
                if (response.isSuccessful) {
                    val animeName = response.body()?.data?.anime?.info?.name
                    animeTitleText.text = "$animeName"
                }
            }
            
            override fun onFailure(call: Call<AnimeResponse>, t: Throwable) {
                Toast.makeText(context, "Failed to load anime details", Toast.LENGTH_SHORT).show()
            }
        })
        
        apiService.getAnimeEpisodes(animeId).enqueue(object : Callback<EpisodeResponse> {
            override fun onResponse(call: Call<EpisodeResponse>, response: Response<EpisodeResponse>) {
                if (response.isSuccessful) {
                    val episodes = response.body()?.data?.episodes ?: return
                    val episode = episodes.find { it.episodeId == episodeId }
                    
                    if (episode != null) {
                        episodeTitleText.text = "${episode.title}"
                        episodeNumberText.text = "Episode: ${episode.number}"
                    }
                }
            }
            
            override fun onFailure(call: Call<EpisodeResponse>, t: Throwable) {
                Toast.makeText(context, "Failed to load episode details", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    interface EpisodeDetailsCallback {
        fun onDetailsLoaded(animeTitle: String, episodeTitle: String, episodeNumber: Int)
        fun onError(message: String)
    }
    
    fun fetchEpisodeDetailsWithCallback(
        animeId: String,
        episodeId: String,
        callback: EpisodeDetailsCallback
    ) {
        var animeTitle = ""
        var episodeTitle = ""
        var episodeNumber = 0
        var animeDetailsLoaded = false
        var episodeDetailsLoaded = false
        
        apiService.getAnimeDetails(animeId).enqueue(object : Callback<AnimeResponse> {
            override fun onResponse(call: Call<AnimeResponse>, response: Response<AnimeResponse>) {
                if (response.isSuccessful) {
                    animeTitle = response.body()?.data?.anime?.info?.name ?: ""
                    animeDetailsLoaded = true
                    
                    if (episodeDetailsLoaded) {
                        callback.onDetailsLoaded(animeTitle, episodeTitle, episodeNumber)
                    }
                } else {
                    callback.onError("Failed to load anime details")
                }
            }
            
            override fun onFailure(call: Call<AnimeResponse>, t: Throwable) {
                callback.onError("Failed to load anime details: ${t.message}")
            }
        })
        
        apiService.getAnimeEpisodes(animeId).enqueue(object : Callback<EpisodeResponse> {
            override fun onResponse(call: Call<EpisodeResponse>, response: Response<EpisodeResponse>) {
                if (response.isSuccessful) {
                    val episodes = response.body()?.data?.episodes ?: return
                    val episode = episodes.find { it.episodeId == episodeId }
                    
                    if (episode != null) {
                        episodeTitle = episode.title ?: ""
                        episodeNumber = episode.number ?: 0
                        episodeDetailsLoaded = true
                        
                        if (animeDetailsLoaded) {
                            callback.onDetailsLoaded(animeTitle, episodeTitle, episodeNumber)
                        }
                    } else {
                        callback.onError("Episode not found")
                    }
                } else {
                    callback.onError("Failed to load episode details")
                }
            }
            
            override fun onFailure(call: Call<EpisodeResponse>, t: Throwable) {
                callback.onError("Failed to load episode details: ${t.message}")
            }
        })
    }
}