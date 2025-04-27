package com.anime.aniwatch.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.anime.aniwatch.AnimeAboutActivity
import com.anime.aniwatch.R
import com.anime.aniwatch.adapter.DateAdapter
import com.anime.aniwatch.adapter.ScheduleAdapter
import com.anime.aniwatch.data.DateItem
import com.anime.aniwatch.data.ScheduledAnime
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class ScheduleFragment : Fragment() {
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var refreshButton: Button
    private lateinit var datesRecyclerView: RecyclerView
    private var scheduleList: MutableList<ScheduledAnime> = mutableListOf()
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var dateAdapter: DateAdapter
    private var dateItems: MutableList<DateItem> = mutableListOf()
    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)

        listView = view.findViewById(R.id.listview_schedule)
        progressBar = view.findViewById(R.id.progress_bar)
        errorTextView = view.findViewById(R.id.textview_error)
        dateTextView = view.findViewById(R.id.textview_date)
        refreshButton = view.findViewById(R.id.btn_refresh)
        datesRecyclerView = view.findViewById(R.id.recyclerview_dates)

        // Initialize schedule adapter
        scheduleAdapter = ScheduleAdapter(
            requireContext(),
            scheduleList,
            onItemClick = { anime ->
                navigateToAnimeAboutActivity(anime)
            }
        )

        listView.adapter = scheduleAdapter

        refreshButton.setOnClickListener {
            fetchSchedule(selectedDate)
        }

        // Generate date items for the next 7 days
        generateDateItems()

        // Initialize date adapter
        dateAdapter = DateAdapter(
            requireContext(),
            dateItems,
            onDateSelected = { dateItem ->
                selectedDate = dateItem.formattedDate
                dateTextView.text = "Schedule for ${dateItem.formattedDate}"
                fetchSchedule(dateItem.formattedDate)
            }
        )

        datesRecyclerView.adapter = dateAdapter

        // Select today's date by default
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val todayIndex = currentDay - 1 // Adjust for 0-based index
        if (todayIndex >= 0 && todayIndex < dateItems.size) {
            dateAdapter.updateSelection(todayIndex)
            // Scroll to today's date
            datesRecyclerView.post {
                datesRecyclerView.scrollToPosition(todayIndex)
            }
        } else {
            dateAdapter.updateSelection(0)
        }

        return view
    }

    private fun generateDateItems() {
        dateItems.clear()
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Set calendar to the first day of the current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // Day of week (e.g., "Mon")
        val dateFormat = SimpleDateFormat("d", Locale.getDefault()) // Day of month (e.g., "15")
        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // API date format

        // Get the maximum number of days in the current month
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Generate date items for all days of the current month
        for (i in 0 until maxDays) {
            val date = calendar.time
            val dayOfWeek = dayFormat.format(date).uppercase()
            val dayOfMonth = dateFormat.format(date)
            val formattedDate = apiDateFormat.format(date)

            val dateItem = DateItem(
                date = date,
                dayOfWeek = dayOfWeek,
                dayOfMonth = dayOfMonth,
                formattedDate = formattedDate,
                isSelected = (i + 1) == currentDay // Select today by default
            )

            dateItems.add(dateItem)

            // Move to the next day
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Set the selected date to today
        val todayIndex = currentDay - 1 // Adjust for 0-based index
        if (todayIndex >= 0 && todayIndex < dateItems.size) {
            selectedDate = dateItems[todayIndex].formattedDate
        } else if (dateItems.isNotEmpty()) {
            selectedDate = dateItems[0].formattedDate
        }
    }

    private fun fetchSchedule(date: String = "") {
        showLoading()

        thread {
            try {
                val apiDate = if (date.isEmpty()) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                } else {
                    date
                }
                val apiUrl = "https://app.jombikbarcenas.site/api/v2/hianime/schedule?date=$apiDate"

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonResponse = response.toString()
                    parseScheduleResponse(jsonResponse)
                } else {
                    activity?.runOnUiThread {
                        showError("Error: HTTP $responseCode")
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun parseScheduleResponse(jsonResponse: String) {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val success = jsonObject.getBoolean("success")

            if (success) {
                val dataObject = jsonObject.getJSONObject("data")
                val animesArray = dataObject.getJSONArray("scheduledAnimes")

                val newScheduleList = mutableListOf<ScheduledAnime>()

                for (i in 0 until animesArray.length()) {
                    val animeObject = animesArray.getJSONObject(i)
                    val anime = ScheduledAnime(
                        id = animeObject.getString("id"),
                        time = animeObject.getString("time"),
                        name = animeObject.getString("name"),
                        jname = animeObject.getString("jname"),
                        airingTimestamp = animeObject.getLong("airingTimestamp"),
                        secondsUntilAiring = animeObject.getLong("secondsUntilAiring"),
                        episode = animeObject.getInt("episode")
                    )
                    newScheduleList.add(anime)
                }

                activity?.runOnUiThread {
                    scheduleList.clear()
                    scheduleList.addAll(newScheduleList)
                    scheduleAdapter.notifyDataSetChanged()
                    showContent()
                }
            } else {
                activity?.runOnUiThread {
                    showError("API returned error")
                }
            }
        } catch (e: Exception) {
            activity?.runOnUiThread {
                showError("Error parsing response: ${e.message}")
            }
        }
    }

    private fun showAnimeDetailsDialog(anime: ScheduledAnime) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(anime.name)

        val message = """
            Japanese Title: ${anime.jname}
            Time: ${anime.time}
            Episode: ${anime.episode}
            ID: ${anime.id}
        """.trimIndent()

        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun navigateToAnimeAboutActivity(anime: ScheduledAnime) {
        val intent = Intent(requireContext(), AnimeAboutActivity::class.java).apply {
            putExtra("ANIME_ID", anime.id)
        }
        startActivity(intent)
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        listView.visibility = View.GONE
        errorTextView.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        listView.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        listView.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = message
    }
}
