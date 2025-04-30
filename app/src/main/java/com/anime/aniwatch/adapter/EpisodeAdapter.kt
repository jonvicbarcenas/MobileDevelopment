package com.anime.aniwatch.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.anime.aniwatch.R
import com.anime.aniwatch.data.WatchlistEpisode
import com.anime.aniwatch.network.Episode
import com.anime.aniwatch.util.WatchHistoryUtil
import com.anime.aniwatch.util.WatchlistUtil
import com.facebook.shimmer.ShimmerFrameLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EpisodeAdapter(
    private var episodes: List<Episode> = emptyList(),
    private val onEpisodeClick: (Episode) -> Unit, // Click listener
    private val animeId: String = "",
    private val animeTitle: String = "",
    private val animePosterUrl: String = ""
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isLoading = true

    companion object {
        private const val VIEW_TYPE_LOADING = 0
        private const val VIEW_TYPE_CONTENT = 1
    }

    class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val episodeNumber: TextView = view.findViewById(R.id.episodeNumber)
        val episodeTitle: TextView = view.findViewById(R.id.episodeTitle)
        val addToWatchlistButton: ImageButton = view.findViewById(R.id.addToWatchlistButton)
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shimmerFrameLayout: ShimmerFrameLayout = view as ShimmerFrameLayout
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading) VIEW_TYPE_LOADING else VIEW_TYPE_CONTENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CONTENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_episode, parent, false)
                EpisodeViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_episode_skeleton, parent, false)
                LoadingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EpisodeViewHolder -> {
                val episode = episodes[position]
                holder.episodeNumber.text = "Episode ${episode.number}"
                holder.episodeTitle.text = episode.title

                holder.itemView.setOnClickListener {
                    onEpisodeClick(episode)
                }

                holder.addToWatchlistButton.setOnClickListener(null)

                WatchHistoryUtil.isInHistory(animeId, episode.episodeId) { isInHistory ->
                    if (holder.adapterPosition == position) {
                        if (isInHistory) {
                            holder.episodeNumber.setTextColor(Color.GRAY)
                            holder.episodeTitle.setTextColor(Color.GRAY)
                        }
                    }
                }

                WatchlistUtil.isInWatchlist("", episode.episodeId) { isInWatchlist ->
                    if (holder.adapterPosition == position) {
                        updateWatchlistButtonState(holder, episode, isInWatchlist)
                    }
                }
            }
            is LoadingViewHolder -> {
                holder.shimmerFrameLayout.startShimmer()
            }
        }
    }

    private fun updateWatchlistButtonState(holder: EpisodeViewHolder, episode: Episode, isInWatchlist: Boolean) {
        val context = holder.itemView.context

        if (isInWatchlist) {
            holder.addToWatchlistButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            holder.addToWatchlistButton.setOnClickListener {
                WatchlistUtil.removeFromWatchlist(context, "", episode.episodeId)
                updateWatchlistButtonState(holder, episode, false)
            }
        } else {
            holder.addToWatchlistButton.setImageResource(android.R.drawable.ic_menu_add)
            holder.addToWatchlistButton.setOnClickListener {
                val watchlistEpisode = WatchlistEpisode(
                    animeId = animeId,
                    animeTitle = animeTitle,
                    episodeId = episode.episodeId,
                    episodeTitle = episode.title,
                    episodeNumber = episode.number,
                    animePosterUrl = animePosterUrl,
                    dateAdded = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )

                WatchlistUtil.saveToWatchlist(context, watchlistEpisode)
                updateWatchlistButtonState(holder, episode, true)
            }
        }
    }


    override fun getItemCount(): Int {
        return if (isLoading) {
            10
        } else {
            episodes.size
        }
    }

    fun updateEpisodes(newEpisodes: List<Episode>) {
        episodes = newEpisodes
        isLoading = false
        notifyDataSetChanged()
    }


    fun setLoading(loading: Boolean) {
        if (isLoading != loading) {
            isLoading = loading
            notifyDataSetChanged()
        }
    }
}
