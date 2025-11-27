package com.example.insighted.ui.report

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.insighted.ui.calendar.BottomNavItem
import com.example.insighted.viewmodels.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    onAttendanceClick: () -> Unit,
    onReportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    userViewModel: UserViewModel
) {
    val uid = userViewModel.uuid.value ?: return

    var daysPresent by remember { mutableStateOf(0) }
    var daysAbsent by remember { mutableStateOf(0) }
    var percent by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    // Fetch data from Firebase
    LaunchedEffect(uid) {
        try {
            loading = true
            val attendanceRef = FirebaseDatabase.getInstance().reference.child("attendance")
            val snapshot = attendanceRef.get().await()

            var presentCount = 0
            var absentCount = 0

            for (dateSnap in snapshot.children) {
                for (periodSnap in dateSnap.children) {
                    val userNode = periodSnap.child(uid)
                    val status = userNode.child("status").getValue(String::class.java)
                    if (status == "present") presentCount++
                    else absentCount++
                }
            }

            daysPresent = presentCount
            daysAbsent = absentCount
            percent = if (presentCount + absentCount > 0) ((presentCount * 100) / (presentCount + absentCount)) else 0
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EDE3))
    ) {
        TopAppBar(
            title = { Text("Report") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {/* Refresh logic if needed */}) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF232F6B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your Report", color = Color.White, style = typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (loading) {
                    Text("Loading...", color = Color.White)
                } else {
                    Text("Total no. of Periods present : $daysPresent", color = Color.White)
                    Text("Total no. of Periods absent : $daysAbsent", color = Color.White)
                    Text("Attendance Percentage : $percent%", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxSize()) {
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
}
