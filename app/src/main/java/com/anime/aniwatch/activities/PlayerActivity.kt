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
import com.anime.aniwatch.fragment.CommentsFragment
import com.anime.aniwatch.network.Track
import com.anime.aniwatch.player.EpisodeDetailsManager
import com.anime.aniwatch.player.EpisodeSourceFetcher
import com.anime.aniwatch.player.FullscreenManager
import com.anime.aniwatch.player.PlayerManager
import com.anime.aniwatch.player.WatchHistoryManager
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: StyledPlayerView
    private lateinit var playerManager: PlayerManager
    private lateinit var fullscreenManager: FullscreenManager
    private lateinit var episodeSourceFetcher: EpisodeSourceFetcher
    private lateinit var watchHistoryManager: WatchHistoryManager
    private lateinit var episodeDetailsManager: EpisodeDetailsManager
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var currentEpisodeId: String? = null
    private var currentAnimeId: String? = null
    private var pagerAdapter: PlayerPagerAdapter? = null
    private var commentsCountListener: ValueEventListener? = null

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
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

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
        currentEpisodeId = episodeId
        currentAnimeId = animeId

        if (episodeId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid episode ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchEpisodeSources(episodeId)
        setupViewPager(animeId.toString(), episodeId)
        fetchCommentsCount(episodeId)

        if (animeId != null && episodeId != null) {
            episodeDetailsManager.fetchEpisodeDetailsWithCallback(
                animeId,
                episodeId,
                object : EpisodeDetailsManager.EpisodeDetailsCallback {
                    override fun onDetailsLoaded(animeTitle: String, episodeTitle: String, episodeNumber: Int) {
                        animeTitleText.text = animeTitle
                        episodeTitleText.text = episodeTitle
                        episodeNumberText.text = "Episode: $episodeNumber"
                        
                        // Update episode title in the pager adapter
                        pagerAdapter?.updateEpisodeTitle(episodeTitle)
                        // Refresh the ViewPager to update the CommentsFragment with the new episode title
                        viewPager.adapter = pagerAdapter

                        Toast.makeText(this@PlayerActivity, "You Are Watching $animeTitle", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(message: String) {
                        Toast.makeText(this@PlayerActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun setupViewPager(animeId: String, episodeId: String) {
        pagerAdapter = PlayerPagerAdapter(this, animeId, episodeId)
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Episodes"
                1 -> "Comments"
                else -> null
            }
        }.attach()
    }

    private fun fetchCommentsCount(episodeId: String) {
        val database = FirebaseDatabase.getInstance()
        val commentsRef = database.getReference("comments").child(episodeId)
        
        commentsCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                updateCommentsTabText(count)
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep default tab text if there's an error
            }
        }
        
        commentsRef.addValueEventListener(commentsCountListener!!)
    }
    
    private fun updateCommentsTabText(count: Long) {
        if (tabLayout.tabCount >= 2) {
            val tab = tabLayout.getTabAt(1)
            tab?.text = "Comments ($count)"
        }
    }

    private class PlayerPagerAdapter(
        activity: FragmentActivity,
        private val animeId: String,
        private val episodeId: String
    ) : FragmentStateAdapter(activity) {

        private var episodeTitle: String = ""
        
        fun updateEpisodeTitle(title: String) {
            this.episodeTitle = title
        }

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> EpisodeFragment().apply {
                    arguments = Bundle().apply {
                        putString("ANIME_ID", animeId)
                        putString("CURRENT_EPISODE_ID", episodeId)
                    }
                }
                1 -> CommentsFragment().apply {
                    arguments = Bundle().apply {
                        putString("EPISODE_ID", episodeId)
                        putString("ANIME_ID", animeId)
                        putString("EPISODE_TITLE", episodeTitle)
                    }
                }
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
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
            currentEpisodeId = newEpisodeId
            currentAnimeId = newAnimeId

            fetchEpisodeSources(newEpisodeId)
            setupViewPager(newAnimeId, newEpisodeId)
            fetchCommentsCount(newEpisodeId)

            episodeDetailsManager.fetchEpisodeDetailsWithCallback(
                newAnimeId,
                newEpisodeId,
                object : EpisodeDetailsManager.EpisodeDetailsCallback {
                    override fun onDetailsLoaded(animeTitle: String, episodeTitle: String, episodeNumber: Int) {
                        findViewById<TextView>(R.id.animeTitleText).text = animeTitle
                        findViewById<TextView>(R.id.episodeTitleText).text = episodeTitle
                        findViewById<TextView>(R.id.episodeNumberText).text = "Episode: $episodeNumber"
                        
                        // Update episode title in the pager adapter
                        pagerAdapter?.updateEpisodeTitle(episodeTitle)
                        // Refresh the ViewPager to update the CommentsFragment with the new episode title
                        viewPager.adapter = pagerAdapter

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
        commentsCountListener?.let { 
            if (currentEpisodeId != null) {
                FirebaseDatabase.getInstance().getReference("comments").child(currentEpisodeId!!)
                    .removeEventListener(it)
            }
        }
    }

    override fun onBackPressed() {
        if (!fullscreenManager.handleBackPressed()) {
            super.onBackPressed()
        }
    }
}
