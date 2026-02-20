package com.example.insighted.ui.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onAttendanceClick: () -> Unit,
    onReportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val minMonth = YearMonth.of(2025, 8)
    val maxMonth = YearMonth.of(2026, 4)

    var currentYearMonth by remember { mutableStateOf(YearMonth.now().coerceIn(minMonth, maxMonth)) }

    // Each set is defined per month
    val workingDaysMap = mapOf(
        YearMonth.of(2025,8) to setOf(20,21,22,23,25,26,28,29,30),
        YearMonth.of(2025, 9) to setOf(1,2,3,8,9,10,11,12,13,15,16,17,18,19,22,24,25,26,27,29,30),
        YearMonth.of(2025, 10) to setOf(6,7,9,10,11,13,14,15,16,17,23,24,25,27,28,29,31),
        YearMonth.of(2025, 11) to setOf(3,4,5,6,7,8,11,12,13,14,17,18,20,21,22,24,25,26,27,28,29),
        YearMonth.of(2025, 12) to setOf(1,2,3,4,5,9,10,11,12,13,15,16,17,18,19,22,23,24,26,27,29,30,31),
        YearMonth.of(2026, 1) to setOf(5,6,7,8,9,19,20,21,22,23,24,27,28,29,30,31),
        YearMonth.of(2026, 2) to setOf(2,3,4,5,6,7,9,10,11,12,13,14,16,17,18,19,20,23,24,25,27,28),
        YearMonth.of(2026, 3) to setOf(2,3,4,5,6,7,9,10,11,12,13,14,16,17,18,23,25,26,27,28,30),
        YearMonth.of(2026, 4) to setOf(1,2,4,6,7,8,9,10,11,15,16,20,22,24,25,27,28,29,30),
        // Add for each month...
    )
    val holidaysMap = mapOf(
        YearMonth.of(2025,8) to setOf(24,27,31),
        YearMonth.of(2025, 9) to setOf(4,5,6,7,14,20,21,28),
        YearMonth.of(2025, 10) to setOf(1,2,3,4,5,12,18,19,20,21,22,26),
        YearMonth.of(2025, 11) to setOf(1,2,9,15,16,23,30),
        YearMonth.of(2025, 12) to setOf(6,7,14,20,21,25,28),
        YearMonth.of(2026, 1) to setOf(1,2,3,4,10,11,12,13,14,15,16,17,18,25,26),
        YearMonth.of(2026, 2) to setOf(1,8,15,21,22),
        YearMonth.of(2026, 3) to setOf(1,8,15,19,20,21,22,29),
        YearMonth.of(2026, 4) to setOf(3,5,12,13,14,19,26),
        // ...
    )
    val examsEventsMap = mapOf(
        YearMonth.of(2025,8) to setOf(),
        YearMonth.of(2025, 9) to setOf(23),
        YearMonth.of(2025, 10) to setOf(8,30),
        YearMonth.of(2025, 11) to setOf(10,19),
        YearMonth.of(2025, 12) to setOf(8),
        YearMonth.of(2026, 1) to setOf(),
        YearMonth.of(2026, 2) to setOf(26),
        YearMonth.of(2026, 3) to setOf(24),
        YearMonth.of(2026, 4) to setOf(17,18,21,23),

        // ...
    )

    val workingDays = workingDaysMap[currentYearMonth] ?: emptySet()
    val holidays = holidaysMap[currentYearMonth] ?: emptySet()
    val examsEvents = examsEventsMap[currentYearMonth] ?: emptySet()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5EDE3))
        ) {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh profile data logic here */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
            // Card with calendar and controls
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232F6B)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Academic Calendar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${currentYearMonth.year}",
                            color = Color.White,
                            style = typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { if (currentYearMonth > minMonth) currentYearMonth = currentYearMonth.minusMonths(1) },
                            enabled = currentYearMonth > minMonth
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Prev", tint = if (currentYearMonth > minMonth) Color.White else Color.LightGray)
                        }
                        IconButton(
                            onClick = { if (currentYearMonth < maxMonth) currentYearMonth = currentYearMonth.plusMonths(1) },
                            enabled = currentYearMonth < maxMonth
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = if (currentYearMonth < maxMonth) Color.White else Color.LightGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                    // Calendar grid
                    val firstDayOfMonth = currentYearMonth.atDay(1)
                    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday=0
                    val daysInMonth = currentYearMonth.lengthOfMonth()

                    Column {
                        for (week in 0 until 6) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (day in 0 until 7) {
                                    val dayNumber = week * 7 + day - dayOfWeekOffset + 1
                                    val bgColor = when {
                                        dayNumber in workingDays -> Color(0xFF7282E5)
                                        dayNumber in holidays -> Color(0xFFF53F3F)
                                        dayNumber in examsEvents -> Color(0xFF9B1CB2)
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .background(
                                                color = if (dayNumber in 1..daysInMonth) bgColor else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNumber in 1..daysInMonth) {
                                            Text(
                                                text = dayNumber.toString(),
                                                color = if (bgColor == Color.Transparent) Color.White else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Legend & totals
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(12.dp)
                            .background(Color(0xFF7282E5), shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Total working days : ${workingDays.size}", color = Color.Black)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(12.dp)
                            .background(Color(0xFFF53F3F), shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Total Holidays      : ${holidays.size}", color = Color.Black)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(12.dp)
                            .background(Color(0xFF9B1CB2), shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Exams & Events      : ${examsEvents.size}", color = Color.Black)
                }
            }
        }

        // Floating Bottom Bar
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .fillMaxWidth(0.95f)
                .height(72.dp),
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3F51B5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(Icons.Default.CheckCircle, "Attendance", onAttendanceClick)
                BottomNavItem(Icons.Default.Analytics, "Report", onReportClick)
                BottomNavItem(Icons.Default.DateRange, "Calendar", onCalendarClick)
                BottomNavItem(Icons.Default.Person, "Profile", onProfileClick)
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
