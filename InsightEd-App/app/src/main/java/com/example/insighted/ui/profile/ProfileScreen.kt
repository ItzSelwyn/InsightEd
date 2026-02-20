package com.example.insighted.ui.profile

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.insighted.viewmodels.UserSession
import com.example.insighted.viewmodels.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onAttendanceClick: () -> Unit,
    onReportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,   // ✅ NEW PARAMETER
    userViewModel: UserViewModel
) {

    val database = FirebaseDatabase.getInstance().reference

    var studentName by remember { mutableStateOf("Loading...") }
    var department by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var registerNo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val uid = UserSession.uuid ?: return

    LaunchedEffect(Unit) {
        try {
            val snapshot = database.child("users").child(uid).get().await()
            val snapshot1 = database.child("students").child(uid).get().await()

            studentName = snapshot1.child("name").getValue(String::class.java) ?: "No Name"
            department = snapshot1.child("dept").getValue(String::class.java) ?: ""
            batch = snapshot1.child("batch").getValue(String::class.java) ?: ""
            email = snapshot.child("email").getValue(String::class.java) ?: ""
            registerNo = snapshot1.child("reg_no").getValue(String::class.java) ?: ""
            password = snapshot.child("password").getValue(String::class.java) ?: ""

        } catch (e: Exception) {
            studentName = "Error loading"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EDE3))
    ) {

        TopAppBar(
            title = { Text("Profile") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {
                    // Optional refresh logic
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
        ) {

            Text(studentName, style = typography.headlineMedium, color = Color.Black)
            Text("$department $batch", style = typography.titleMedium, color = Color.Black)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232F6B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Email ID        : $email", color = Color.White)
                    Text("Register No   : $registerNo", color = Color.White)
                    Text("Password       : $password", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🔴 LOGOUT BUTTON
            Button(
                onClick = { onLogout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Logout", color = Color.White)
            }
        }

        // Bottom Navigation
        Card(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth(0.95f)
                .height(72.dp)
                .align(Alignment.CenterHorizontally),
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