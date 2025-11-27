package com.example.insighted.ui.attendance

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.insighted.viewmodels.UserViewModel
import com.example.insighted.viewmodels.periodTimings
import com.example.insighted.viewmodels.getCurrentPeriodNumber
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Period(
    val number: Int,
    val subject: String,
    val timing: String,
    var attendanceStatus: String = "Loading..."
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(onBack: () -> Unit, userViewModel: UserViewModel) {
    val database = FirebaseDatabase.getInstance().reference
    val uid = userViewModel.uuid.value ?: return

    // Determine today's date as dd_MM_yyyy
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"))

    var periods by remember {
        mutableStateOf(
            periodTimings.map { periodTime ->
                Period(
                    number = periodTime.number,
                    subject = "",  // To be fetched dynamically
                    timing = "${periodTime.start} - ${periodTime.end}",
                    attendanceStatus = "Loading..."
                )
            }
        )
    }

    val currentPeriodNo = getCurrentPeriodNumber()

    LaunchedEffect(currentPeriodNo, today, uid) {
        try {
            // Fetch subjects from 'periods'
            val subjectsSnap = database.child("periods").get().await()
            val updatedPeriods = periods.map { period ->
                val subjectName = subjectsSnap.child("period${period.number}").getValue(String::class.java) ?: ""
                period.copy(subject = subjectName)
            }
            // Fetch attendance status for each period from the required path
            val finalPeriods = updatedPeriods.map { period ->
                val statusFromDb = database.child("attendance")
                    .child(today)
                    .child("period${period.number}")
                    .child(uid)
                    .child("status")
                    .get()
                    .await()
                    .getValue(String::class.java) ?: "Pending"
                period.copy(attendanceStatus = statusFromDb)
            }
            periods = finalPeriods
        } catch (e: Exception) {
            periods = periods.map { it.copy(attendanceStatus = "Error") }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EDE3))
    ) {
        TopAppBar(
            title = { Text("Attendance") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { /* Refresh logic if needed */ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Scrollable list of period cards
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            periods.forEach { period ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp, horizontal = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232F6B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 18.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Period ${period.number}", color = Color.White, style = typography.titleMedium)
                        Text("Subject : ${period.subject.ifBlank { "No subject assigned" }}", color = Color.White, style = typography.bodyLarge)
                        Text("Timing : ${period.timing}", color = Color.White, style = typography.bodyLarge)
                        Text(
                            "Attendance : ${period.attendanceStatus}",
                            color = when (period.attendanceStatus.lowercase()) {
                                "present", "enrolled" -> Color(0xFF25D366)
                                "absent" -> Color.Red
                                "pending" -> Color.Yellow
                                "error" -> Color.Gray
                                else -> Color.White
                            },
                            style = typography.bodyLarge
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp)) // padding at the end so last card is not cut off
        }
    }
}
