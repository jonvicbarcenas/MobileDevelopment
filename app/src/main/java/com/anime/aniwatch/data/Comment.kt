package com.anime.aniwatch.data

data class Comment(
    val id: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val episodeId: String = "",
    val animeId: String = "",
    val profileImageUrl: String = ""
) 