package com.anime.aniwatch

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anime.aniwatch.fragment.EpisodeFragment
import com.anime.aniwatch.network.AnimeDetails
import com.anime.aniwatch.network.AnimeResponse
import com.anime.aniwatch.network.ApiService
import com.anime.aniwatch.network.Season
import com.anime.aniwatch.util.Constants
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AnimeAboutActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var thumbnail: ImageView
    private lateinit var titleText: TextView
    private lateinit var seasonText: TextView
    private lateinit var genreText: TextView
    private lateinit var episodeCountText: TextView
    private lateinit var ratingText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var toggleDescriptionButton: Button


    private var isDescriptionExpanded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime_about)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        thumbnail = findViewById(R.id.thumbnail)
        titleText = findViewById(R.id.titleText)
        seasonText = findViewById(R.id.seasonText)
        genreText = findViewById(R.id.genreText)
        episodeCountText = findViewById(R.id.episodeCountText)
        ratingText = findViewById(R.id.ratingText)
        descriptionText = findViewById(R.id.descriptionText)
        toggleDescriptionButton = findViewById(R.id.toggleDescriptionButton)


        backButton.setOnClickListener { finish() }

        val animeId = intent.getStringExtra("ANIME_ID") ?: return

        fetchAnimeDetails(animeId)

        loadEpisodeFragment(animeId)

        toggleDescriptionButton.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                descriptionText.maxLines = Int.MAX_VALUE
                toggleDescriptionButton.text = "Show Less"
            } else {
                descriptionText.maxLines = 3
                toggleDescriptionButton.text = "Show More"
            }
        }
    }

    private fun fetchAnimeDetails(animeId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        apiService.getAnimeDetails(animeId).enqueue(object : Callback<AnimeResponse> {
            override fun onResponse(call: Call<AnimeResponse>, response: Response<AnimeResponse>) {
                if (response.isSuccessful) {
                    val animeData = response.body()?.data ?: return
                    displayAnimeDetails(animeData.anime, animeData.seasons)
                } else {
                    Toast.makeText(this@AnimeAboutActivity, "Failed to load anime details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AnimeResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@AnimeAboutActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayAnimeDetails(anime: AnimeDetails, seasons: List<Season>) {
        if (isDestroyed || isFinishing) return

        Glide.with(this)
            .load(anime.info.poster)
            .into(thumbnail)

        titleText.text = anime.info.name

        val currentSeason = seasons.find { it.isCurrent } ?: seasons.firstOrNull()
        seasonText.text = currentSeason?.title ?: "No Season"

        genreText.text = "Genre: ${anime.moreInfo.genres.joinToString(", ")}"
        episodeCountText.text = "Episodes: Sub: ${anime.info.stats.episodes.sub}, Dub: ${anime.info.stats.episodes.dub}"
        ratingText.text = "${anime.moreInfo.malscore} ★"
        descriptionText.text = anime.info.description
    }


    private fun loadEpisodeFragment(animeId: String) {
        val episodeFragment = EpisodeFragment().apply {
            arguments = Bundle().apply {
                putString("ANIME_ID", animeId)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.episodeFragmentContainer, episodeFragment)
            .commit()
    }
}