package com.anime.aniwatch.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anime.aniwatch.AnimeAboutActivity
import com.anime.aniwatch.data.Anime
import com.anime.aniwatch.databinding.SearchBinding
import com.anime.aniwatch.adapter.SearchAdapter
import com.anime.aniwatch.network.ApiService
import com.anime.aniwatch.network.SearchResponse
import com.anime.aniwatch.util.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchActivity : AppCompatActivity() {

    private lateinit var listOfMovies: MutableList<Anime>
    private lateinit var movieAdapter: SearchAdapter
    private lateinit var binding: SearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backButton: ImageView = binding.backButton
        backButton.setOnClickListener { onBackPressed() }

        val listView: ListView = binding.list
        val searchView: SearchView = binding.search

        listOfMovies = mutableListOf()
        movieAdapter = SearchAdapter(this, listOfMovies) { movie: Anime ->
            val intent = Intent(this, AnimeAboutActivity::class.java).apply {
                putExtra("ANIME_ID", movie.id)
            }
            startActivity(intent)
        }
        listView.adapter = movieAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    searchAnime(newText)
                }
                return true
            }
        })
    }

    private fun searchAnime(query: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        apiService.searchAnime(query).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                if (response.isSuccessful) {
                    val animes = response.body()?.data?.animes ?: emptyList()
                    movieAdapter.updateList(animes)
                } else {
                    Toast.makeText(this@SearchActivity, "Failed to fetch results", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@SearchActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}