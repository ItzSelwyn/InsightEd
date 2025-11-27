package com.example.insighted.ui.dashboard

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.insighted.R
import com.example.insighted.viewmodels.UserViewModel
import com.example.insighted.viewmodels.getCurrentPeriodNumber
import com.example.insighted.viewmodels.periodTimings
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.xr.compose.testing.toDp
import com.example.insighted.ble.BleAdvertiser
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    onAttendanceClick: () -> Unit,
    onReportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val uid = userViewModel.uuid.value ?: return

    val bleAdvertiser = remember { BleAdvertiser(context) }

    // Start advertising when UID is available
    LaunchedEffect(uid) {
        bleAdvertiser.startAdvertising(uid)
    }

    var studentName by remember { mutableStateOf("Loading...") }
    var department by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf("Loading...") }

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"))
    val periodNo = getCurrentPeriodNumber()
    val periodTime = periodTimings.find { it.number == periodNo }
    var periodSubject by remember { mutableStateOf<String?>(null) }

    // Fetch subject from Firebase (kept same)
    LaunchedEffect(periodNo) {
        if (periodNo != null) {
            val ref = FirebaseDatabase.getInstance().reference.child("periods")
            val data = ref.get().await()
            val subject = data.child("period$periodNo").getValue(String::class.java)
            periodSubject = subject
        }
    }

    // Fetch student details (kept same)
    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseDatabase.getInstance().reference
                .child("students").child(uid).get().await()
            val snapshot1 = FirebaseDatabase.getInstance().reference
                .child("attendance").get().await()
            studentName = snapshot.child("name").getValue(String::class.java) ?: "Name"
            department = snapshot.child("dept").getValue(String::class.java) ?: ""
            batch = snapshot.child("batch").getValue(String::class.java) ?: ""
            currentStatus = snapshot1
                .child(today)
                .child("period${periodNo}")
                .child(uid)
                .child("status")
                .getValue(String::class.java) ?: "Pending"
        } catch (e: Exception) {
            studentName = "Error loading"
            currentStatus = "Error loading"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EDE3))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Banner
            Box {
                Image(
                    painter = painterResource(id = R.drawable.top),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                ) {
                    Text("Welcome!", color = Color.White, style = typography.headlineLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(studentName, color = Color.White, style = typography.titleLarge)
                    Text("$department $batch", color = Color.White, style = typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Current Activity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232F6B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Current Activity", color = Color.White, style = typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (periodNo != null && periodTime != null && periodSubject != null) {
                        InfoRow("Period No", periodTime.number.toString())
                        InfoRow("Subject", periodSubject ?: "No Subject")
                        InfoRow("Timing", "${periodTime.start} - ${periodTime.end}")
                        InfoRow("Status", currentStatus)
                    } else {
                        Text("No current period active", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Floating Rounded Bottom Nav
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
fun InfoRow(label: String, value: String) {
    Row {
        Text("$label : ", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
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

