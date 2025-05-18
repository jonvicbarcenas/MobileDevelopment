package com.anime.aniwatch.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anime.aniwatch.R
import com.anime.aniwatch.activities.PlayerActivity
import com.anime.aniwatch.adapter.EpisodeAdapter
import com.anime.aniwatch.network.AnimeResponse
import com.anime.aniwatch.network.ApiService
import com.anime.aniwatch.network.Episode
import com.anime.aniwatch.network.EpisodeResponse
import com.anime.aniwatch.util.Constants
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EpisodeFragment : Fragment() {

    private lateinit var episodeAdapter: EpisodeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager

    private var animeTitle: String = ""
    private var animePosterUrl: String = ""
    private var currentlyPlayingEpisodeId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_episode, container, false)
        recyclerView = view.findViewById(R.id.episodeRecyclerView)
        layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        val animeId = arguments?.getString("ANIME_ID") ?: ""
        currentlyPlayingEpisodeId = arguments?.getString("CURRENT_EPISODE_ID")

        // Fetch anime details first to get title and poster URL
        fetchAnimeDetails(animeId)

        return view
    }

    private fun fetchAnimeDetails(animeId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        apiService.getAnimeDetails(animeId).enqueue(object : Callback<AnimeResponse> {
            override fun onResponse(call: Call<AnimeResponse>, response: Response<AnimeResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val animeInfo = response.body()?.data?.anime?.info
                    if (animeInfo != null) {
                        animeTitle = animeInfo.name
                        animePosterUrl = animeInfo.poster

                        // Now that we have the anime details, fetch episodes
                        setupEpisodeAdapter(animeId)
                        fetchEpisodes(animeId)
                    }
                } else {
                    Toast.makeText(context, "Failed to load anime details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AnimeResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupEpisodeAdapter(animeId: String) {
        episodeAdapter = EpisodeAdapter(
            episodes = emptyList(),
            onEpisodeClick = { episode ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("EPISODE_ID", episode.episodeId)
                    putExtra("ANIME_ID", animeId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            },
            animeId = animeId,
            animeTitle = animeTitle,
            animePosterUrl = animePosterUrl
        )
        // Explicitly set loading state to true to show skeleton loader
        episodeAdapter.setLoading(true)
        recyclerView.adapter = episodeAdapter
    }

    private fun fetchEpisodes(animeId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        apiService.getAnimeEpisodes(animeId).enqueue(object : Callback<EpisodeResponse> {
            override fun onResponse(call: Call<EpisodeResponse>, response: Response<EpisodeResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val episodes = response.body()?.data?.episodes ?: emptyList()
                    // updateEpisodes automatically sets isLoading to false
                    episodeAdapter.updateEpisodes(episodes)
                    
                    // Set currently playing episode and scroll to it
                    currentlyPlayingEpisodeId?.let { episodeId ->
                        episodeAdapter.setCurrentlyPlayingEpisode(episodeId)
                        scrollToCurrentEpisode(episodes, episodeId)
                    }
                } else {
                    // Set loading to false even if there's an error
                    episodeAdapter.setLoading(false)
                    Toast.makeText(context, "Failed to load episodes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<EpisodeResponse>, t: Throwable) {
                // Set loading to false on network failure
                episodeAdapter.setLoading(false)
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun scrollToCurrentEpisode(episodes: List<Episode>, episodeId: String) {
        val index = episodes.indexOfFirst { it.episodeId == episodeId }
        if (index >= 0) {
            layoutManager.scrollToPositionWithOffset(index, 20)
        }
    }
    
    fun updateCurrentlyPlayingEpisode(episodeId: String) {
        currentlyPlayingEpisodeId = episodeId
        if (::episodeAdapter.isInitialized) {
            episodeAdapter.setCurrentlyPlayingEpisode(episodeId)
            
            // Get the current episodes from the adapter
            val episodes = mutableListOf<Episode>()
            for (i in 0 until episodeAdapter.itemCount) {
                if (!episodeAdapter.getItemViewType(i).equals(0)) { // Not loading view type
                    // This is a bit of a hack since we don't have direct access to the episodes list
                    // In a real app, you might want to refactor to make this cleaner
                    try {
                        val episode = episodeAdapter.javaClass.getDeclaredField("episodes")
                            .apply { isAccessible = true }
                            .get(episodeAdapter) as? List<Episode>
                        if (episode != null) {
                            episodes.addAll(episode)
                            break
                        }
                    } catch (e: Exception) {
                        // Handle reflection exception
                    }
                }
            }
            
            scrollToCurrentEpisode(episodes, episodeId)
        }
    }
}
