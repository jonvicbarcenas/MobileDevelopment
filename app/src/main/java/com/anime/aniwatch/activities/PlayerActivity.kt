package com.anime.aniwatch.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anime.aniwatch.R
import android.content.res.Configuration
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.anime.aniwatch.fragment.EpisodeFragment
import com.anime.aniwatch.network.Track
import com.anime.aniwatch.player.EpisodeDetailsManager
import com.anime.aniwatch.player.EpisodeSourceFetcher
import com.anime.aniwatch.player.FullscreenManager
import com.anime.aniwatch.player.PlayerManager
import com.anime.aniwatch.player.WatchHistoryManager
import com.google.android.exoplayer2.ui.StyledPlayerView
import android.widget.ImageView


class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: StyledPlayerView
    private lateinit var playerManager: PlayerManager
    private lateinit var fullscreenManager: FullscreenManager
    private lateinit var episodeSourceFetcher: EpisodeSourceFetcher
    private lateinit var watchHistoryManager: WatchHistoryManager
    private lateinit var episodeDetailsManager: EpisodeDetailsManager
    private var currentEpisodeFragment: EpisodeFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val animeTitleText: TextView = findViewById(R.id.animeTitleText)
        val episodeTitleText: TextView = findViewById(R.id.episodeTitleText)
        val episodeNumberText: TextView = findViewById(R.id.episodeNumberText)
        val nowPlayingLabel: TextView = findViewById(R.id.nowPlayingLabel)

        // Apply pulsing animation to NOW PLAYING label
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.text_pulse_animation)
        nowPlayingLabel.startAnimation(pulseAnimation)

        playerView = findViewById(R.id.playerView)

        playerManager = PlayerManager(this, playerView)
        fullscreenManager = FullscreenManager(this, playerView)
        episodeSourceFetcher = EpisodeSourceFetcher(this)
        watchHistoryManager = WatchHistoryManager(this)
        episodeDetailsManager = EpisodeDetailsManager(this)

        playerView.setControllerOnFullScreenModeChangedListener { isFullScreen ->
            if (isFullScreen) {
                fullscreenManager.enterFullscreen()
            } else {
                fullscreenManager.exitFullscreen()
            }
        }

        val episodeId = intent.getStringExtra("EPISODE_ID")
        val animeId = intent.getStringExtra("ANIME_ID")

        if (episodeId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid episode ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchEpisodeSources(episodeId)

        loadEpisodeFragment(animeId.toString(), episodeId)

        if (animeId != null && episodeId != null) {
            episodeDetailsManager.fetchEpisodeDetailsWithCallback(
                animeId,
                episodeId,
                object : EpisodeDetailsManager.EpisodeDetailsCallback {
                    override fun onDetailsLoaded(animeTitle: String, episodeTitle: String, episodeNumber: Int) {
                        animeTitleText.text = animeTitle
                        episodeTitleText.text = episodeTitle
                        episodeNumberText.text = "Episode: $episodeNumber"

                        Toast.makeText(this@PlayerActivity, "You Are Watching $animeTitle", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(message: String) {
                        Toast.makeText(this@PlayerActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun fetchEpisodeSources(episodeId: String) {
        episodeSourceFetcher.fetchEpisodeSources(episodeId, object : EpisodeSourceFetcher.EpisodeSourceCallback {
            override fun onSourceFetched(hlsUrl: String, tracks: List<Track>, referer: String) {
                val animeId = intent.getStringExtra("ANIME_ID") ?: return

                watchHistoryManager.getWatchedTime(episodeId, animeId) { savedWatchedTime ->
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

            playerManager.stopPlayer()

            this.intent = intent

            fetchEpisodeSources(newEpisodeId)
            
            // Update the currently playing episode in the fragment if it exists
            currentEpisodeFragment?.updateCurrentlyPlayingEpisode(newEpisodeId) ?: run {
                // If fragment doesn't exist yet, load it
                loadEpisodeFragment(newAnimeId, newEpisodeId)
            }

            episodeDetailsManager.fetchEpisodeDetailsWithCallback(
                newAnimeId,
                newEpisodeId,
                object : EpisodeDetailsManager.EpisodeDetailsCallback {
                    override fun onDetailsLoaded(animeTitle: String, episodeTitle: String, episodeNumber: Int) {
                        findViewById<TextView>(R.id.animeTitleText).text = animeTitle
                        findViewById<TextView>(R.id.episodeTitleText).text = episodeTitle
                        findViewById<TextView>(R.id.episodeNumberText).text = "Episode: $episodeNumber"

                        Toast.makeText(this@PlayerActivity, "You Are Watching $animeTitle", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(message: String) {
                        Toast.makeText(this@PlayerActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
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

        playerManager.releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        playerManager.resumePlayer()
        
        // Re-apply animation when resuming
        val nowPlayingLabel: TextView = findViewById(R.id.nowPlayingLabel)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.text_pulse_animation)
        nowPlayingLabel.startAnimation(pulseAnimation)
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

    private fun loadEpisodeFragment(animeId: String, episodeId: String) {
        val episodeFragment = EpisodeFragment().apply {
            arguments = Bundle().apply {
                putString("ANIME_ID", animeId)
                putString("CURRENT_EPISODE_ID", episodeId)
            }
        }
        
        currentEpisodeFragment = episodeFragment

        supportFragmentManager.beginTransaction()
            .replace(R.id.episodeFragmentContainer, episodeFragment)
            .commit()
    }
}
