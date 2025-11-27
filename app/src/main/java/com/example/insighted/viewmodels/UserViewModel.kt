package com.example.insighted.viewmodels
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import java.time.LocalTime

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {
    var uuid = mutableStateOf<String?>(null) // Holds the logged-in user's UUID
}
object UserSession {
    var uuid: String? = null
}

data class PeriodTime(val number: Int, val start: String, val end: String)

val periodTimings = listOf(
    PeriodTime(1, "09:15", "10:15"),
    PeriodTime(2, "10:15", "11:15"),
    PeriodTime(3, "11:45", "12:45"),
    PeriodTime(4, "13:45", "14:40"),
    PeriodTime(5, "14:40", "15:35"),
    PeriodTime(6, "15:35", "16:30")
)

@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentPeriodNumber(): Int? {
    val now = java.time.LocalTime.now()
    for (period in periodTimings) {
        val start = java.time.LocalTime.parse(period.start)
        val end = java.time.LocalTime.parse(period.end)
        if (!now.isBefore(start) && now.isBefore(end)) {
            return period.number
        }
    }
    return null
}

