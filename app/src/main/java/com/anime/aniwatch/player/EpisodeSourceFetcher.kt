package com.anime.aniwatch.player

import android.content.Context
import android.widget.Toast
import com.anime.aniwatch.network.ApiService
import com.anime.aniwatch.network.EpisodeSourceResponse
import com.anime.aniwatch.network.Track
import com.anime.aniwatch.util.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EpisodeSourceFetcher(private val context: Context) {

    interface EpisodeSourceCallback {
        fun onSourceFetched(hlsUrl: String, tracks: List<Track>, referer: String)
        fun onError(message: String)
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    fun fetchEpisodeSources(episodeId: String, callback: EpisodeSourceCallback) {
        apiService.getEpisodeSources(episodeId).enqueue(object : Callback<EpisodeSourceResponse> {
            override fun onResponse(call: Call<EpisodeSourceResponse>, response: Response<EpisodeSourceResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val sourceData = response.body()?.data
                    if (sourceData != null) {
                        val hlsUrl = sourceData.sources.firstOrNull { it.type == "hls" }?.url

                        if (hlsUrl != null) {
                            val referer = sourceData.headers["Referer"] ?: "https://megacloud.blog/"
                            callback.onSourceFetched(hlsUrl, sourceData.tracks, referer)
                        } else {
                            callback.onError("No playable sources found")
                        }
                    }
                } else if (response.code() == 500 && response.errorBody()?.string()?.contains("Couldn't find server") == true) {
                    Toast.makeText(context, "Server not found, trying backup server...", Toast.LENGTH_SHORT).show()
                    // If the first server fails, try the backup server
                    fetchEpisodeSourcesFromBackup(episodeId, callback)
                } else {
                    callback.onError("Failed to load episode sources")
                }
            }

            override fun onFailure(call: Call<EpisodeSourceResponse>, t: Throwable) {
                callback.onError("Error: ${t.message}")
            }
        })
    }

    private fun fetchEpisodeSourcesFromBackup(episodeId: String, callback: EpisodeSourceCallback) {
        apiService.getEpisodeSourcesBackup(episodeId).enqueue(object : Callback<EpisodeSourceResponse> {
            override fun onResponse(call: Call<EpisodeSourceResponse>, response: Response<EpisodeSourceResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val sourceData = response.body()?.data
                    if (sourceData != null) {
                        val hlsUrl = sourceData.sources.firstOrNull { it.type == "hls" }?.url

                        if (hlsUrl != null) {
                            val referer = sourceData.headers["Referer"] ?: "https://megacloud.blog/"
                            callback.onSourceFetched(hlsUrl, sourceData.tracks, referer)
                        } else {
                            callback.onError("No playable sources found")
                        }
                    }
                } else {
                    callback.onError("Failed to load backup episode sources")
                }
            }

            override fun onFailure(call: Call<EpisodeSourceResponse>, t: Throwable) {
                callback.onError("Error: ${t.message}")
            }
        })
    }
}