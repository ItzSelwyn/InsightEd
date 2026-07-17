package com.example.insighted.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDebugScreen(
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    val defaultDate = remember {
        SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Date())
    }

    var date by remember { mutableStateOf(defaultDate) }
    var period by remember { mutableStateOf("period1") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready.") }
    var outputText by remember { mutableStateOf("") }

    suspend fun ensureFirebaseAuthUser(): String {
        val existingUser = auth.currentUser
        if (existingUser != null) {
            return existingUser.uid
        }

        val anonymousResult = auth.signInAnonymously().await()
        return anonymousResult.user?.uid
            ?: throw IllegalStateException("Firebase anonymous sign-in failed")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Cloud Function Runner",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (dd_mm_yyyy)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = period,
                onValueChange = { period = it },
                label = { Text("Period (e.g. period5)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        isLoading = true
                        statusMessage = "Signing in for debug run..."
                        outputText = ""

                        coroutineScope.launch {
                            try {
                                val currentUserUid = withContext(Dispatchers.IO) {
                                    ensureFirebaseAuthUser()
                                }

                                statusMessage = "Running cloud function as $currentUserUid..."

                                val result = withContext(Dispatchers.IO) {
                                    val payload = hashMapOf(
                                        "date" to date.trim(),
                                        "period" to period.trim(),
                                    )

                                    FirebaseFunctions.getInstance()
                                        .getHttpsCallable("processPeriodAttendance")
                                        .call(payload)
                                        .await()
                                }

                                val jsonText = when (val data = result.data) {
                                    is Map<*, *> -> JSONObject(data as Map<*, *>).toString(2)
                                    else -> JSONObject().put("data", data).toString(2)
                                }

                                val processedCount = (result.data as? Map<*, *>)?.get("processed_students")?.toString() ?: "?"
                                val flaggedCount = (result.data as? Map<*, *>)?.get("flagged_students")?.toString() ?: "?"

                                statusMessage = "Completed. Processed $processedCount students, flagged $flaggedCount."
                                outputText = jsonText
                            } catch (error: Exception) {
                                statusMessage = "Error: ${error.message ?: error::class.java.simpleName}"
                                outputText = error.stackTraceToString()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Run AI Processing")
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusMessage,
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .border(1.dp, Color.Black)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = if (outputText.isBlank()) "No output yet." else outputText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
        }
    }
}
