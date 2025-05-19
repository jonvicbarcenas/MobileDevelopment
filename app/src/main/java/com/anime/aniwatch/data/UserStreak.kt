package com.anime.aniwatch.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserStreak(
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastWatchDate: String = "",
    var streakDates: MutableList<String> = mutableListOf()
) {
    constructor() : this(0, 0, "", mutableListOf())
    
    companion object {
        fun getCurrentDateString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }
        
        fun extractDateFromTimestamp(timestamp: String): String {
            try {
                // Try to parse the timestamp format "yyyy-MM-dd HH:mm:ss"
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                
                // Format to just the date part
                if (date != null) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    return dateFormat.format(date)
                }
            } catch (e: Exception) {
                // If parsing fails, check if it's already in the correct format
                if (timestamp.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    return timestamp
                }
                e.printStackTrace()
            }
            
            // Return current date as fallback
            return getCurrentDateString()
        }
        
        fun isConsecutiveDay(lastDate: String, currentDate: String): Boolean {
            if (lastDate.isEmpty()) return true
            
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                // Extract just the date part if timestamps contain time
                val lastDateClean = extractDateFromTimestamp(lastDate)
                val currentDateClean = extractDateFromTimestamp(currentDate)
                
                val lastDateObj = dateFormat.parse(lastDateClean)
                val currentDateObj = dateFormat.parse(currentDateClean)
                
                if (lastDateObj != null && currentDateObj != null) {
                    val calendar = Calendar.getInstance()
                    
                    // Check if it's the same day
                    calendar.time = lastDateObj
                    val lastYear = calendar.get(Calendar.YEAR)
                    val lastDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                    
                    calendar.time = currentDateObj
                    val currentYear = calendar.get(Calendar.YEAR)
                    val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                    
                    // If same day, return true (maintain streak)
                    if (lastYear == currentYear && lastDayOfYear == currentDayOfYear) {
                        return true
                    }
                    
                    // Check if current date is one day after last date
                    calendar.time = lastDateObj
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val nextDay = calendar.time
                    
                    return currentDateObj.compareTo(nextDay) == 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return false
        }
    }
} 