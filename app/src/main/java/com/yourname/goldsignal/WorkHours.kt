package com.yourname.goldsignal

import android.content.SharedPreferences
import java.util.*

class WorkingHoursManager {
    private var startHour: Int = 8
    private var startMinute: Int = 0
    private var endHour: Int = 18
    private var endMinute: Int = 0
    
    fun loadSettings(sharedPreferences: SharedPreferences) {
        startHour = sharedPreferences.getInt("startHour", 8)
        startMinute = sharedPreferences.getInt("startMinute", 0)
        endHour = sharedPreferences.getInt("endHour", 18)
        endMinute = sharedPreferences.getInt("endMinute", 0)
    }
    
    fun isWithinWorkingHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val startTimeInMinutes = startHour * 60 + startMinute
        val endTimeInMinutes = endHour * 60 + endMinute
        
        return currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
    }
}
