package com.anime.aniwatch.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anime.aniwatch.R
import android.content.res.Configuration
import android.widget.TextView
import com.anime.aniwatch.fragment.EpisodeFragment
import com.anime.aniwatch.network.Track
import com.anime.aniwatch.player.EpisodeDetailsManager
import com.anime.aniwatch.player.EpisodeSourceFetcher
import com.anime.aniwatch.player.FullscreenManager
import com.anime.aniwatch.player.PlayerManager
import com.anime.aniwatch.player.WatchHistoryManager
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.SubtitleView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: StyledPlayerView

    
    // Helper classes
    private lateinit var playerManager: PlayerManager
    private lateinit var fullscreenManager: FullscreenManager
    private lateinit var episodeSourceFetcher: EpisodeSourceFetcher
    private lateinit var watchHistoryManager: WatchHistoryManager
    private lateinit var episodeDetailsManager: EpisodeDetailsManager

    private var playbackPosition: Long = 0
    private var playWhenReady: Boolean = true
    private lateinit var backButton: ImageView


    private var isFullscreen = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val animeTitleText: TextView = findViewById(R.id.animeTitleText)
        val episodeTitleText: TextView = findViewById(R.id.episodeTitleText)
        val episodeNumberText: TextView = findViewById(R.id.episodeNumberText)
        
        playerView = findViewById(R.id.playerView)

        
        // Initialize helper classes
        playerManager = PlayerManager(this, playerView)
        fullscreenManager = FullscreenManager(this, playerView)
        episodeSourceFetcher = EpisodeSourceFetcher(this)
        watchHistoryManager = WatchHistoryManager(this)
        episodeDetailsManager = EpisodeDetailsManager(this)
        





        // Set up fullscreen button click listener
        playerView.setControllerOnFullScreenModeChangedListener { isFullScreen ->
            if (isFullScreen) {
                fullscreenManager.enterFullscreen()
            } else {
                fullscreenManager.exitFullscreen()
            }
        }

        // Get episode and anime IDs from intent
        val episodeId = intent.getStringExtra("EPISODE_ID")
        val animeId = intent.getStringExtra("ANIME_ID")
        
        if (episodeId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid episode ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch episode sources
        fetchEpisodeSources(episodeId)
        
        // Load episode fragment
        loadEpisodeFragment(animeId.toString())
        
        // Fetch episode details
        if (animeId != null && episodeId != null) {
            episodeDetailsManager.fetchEpisodeDetails(
                animeId, 
                episodeId, 
                animeTitleText, 
                episodeTitleText, 
                episodeNumberText
            )
        }
        
        Toast.makeText(this, "Anime ID: $animeId", Toast.LENGTH_SHORT).show()
    }

    private fun fetchEpisodeSources(episodeId: String) {
        episodeSourceFetcher.fetchEpisodeSources(episodeId, object : EpisodeSourceFetcher.EpisodeSourceCallback {
            override fun onSourceFetched(hlsUrl: String, tracks: List<Track>, referer: String) {
                // Get watched time from history
                val animeId = intent.getStringExtra("ANIME_ID") ?: return
                
                watchHistoryManager.getWatchedTime(episodeId, animeId) { savedWatchedTime ->
                    // Prepare player with the fetched sources
                    playerManager.preparePlayer(hlsUrl, tracks, referer, savedWatchedTime)
                }
            }

            override fun onError(message: String) {
                Toast.makeText(this@PlayerActivity, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fullscreenManager.handleConfigurationChange(newConfig)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val newEpisodeId = it.getStringExtra("EPISODE_ID") ?: return
            val newAnimeId = it.getStringExtra("ANIME_ID") ?: return

            // Clear the current player state
            playerManager.stopPlayer()

            // Update the intent data
            this.intent = intent

            // Fetch and prepare the new episode
            fetchEpisodeSources(newEpisodeId)
            loadEpisodeFragment(newAnimeId)
            episodeDetailsManager.fetchEpisodeDetails(
                newAnimeId,
                newEpisodeId,
                findViewById(R.id.animeTitleText),
                findViewById(R.id.episodeTitleText),
                findViewById(R.id.episodeNumberText)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        
        // Save watch history
        val episodeId = intent.getStringExtra("EPISODE_ID") ?: return
        val animeId = intent.getStringExtra("ANIME_ID") ?: return
        val animeTitle = findViewById<TextView>(R.id.animeTitleText).text.toString()
        val episodeTitle = findViewById<TextView>(R.id.episodeTitleText).text.toString()
        val episodeNumber = findViewById<TextView>(R.id.episodeNumberText).text.toString()
            .replace("Episode: ", "").toIntOrNull() ?: 0
        
        watchHistoryManager.saveWatchHistory(
            episodeId,
            animeId,
            animeTitle,
            episodeTitle,
            episodeNumber,
            playerManager.getCurrentPosition(),
            playerManager.getDuration()
        )
        
        // Release player
        playerManager.releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        playerManager.resumePlayer()
    }

    override fun onStop() {
        super.onStop()
        playerManager.releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.releasePlayer()
    }

    override fun onBackPressed() {
        if (!fullscreenManager.handleBackPressed()) {
            super.onBackPressed()
        }
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