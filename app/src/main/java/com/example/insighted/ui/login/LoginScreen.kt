package com.example.insighted.ui.login

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import com.example.insighted.R
import com.example.insighted.viewmodels.UserSession
import com.example.insighted.viewmodels.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(onLoginSuccess = {})
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        Image(
            painter = painterResource(id = R.drawable.untitled1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = (-82).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5EDE3))
            ){
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "InsightEd",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5EDE3))
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "User Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF232F6B)
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF546DDC), RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF546DDC), RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    loading = true
                    checkLogin(email, password,
                        onSuccess = { uuid ->
                            loading = false
                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(uuid) // Pass UUID here!
                        },
                        onFailure = {
                            loading = false
                            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546DDC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (loading) "Logging in..." else "Login",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF232F6B)
                )
            }
        }
    }
}

fun checkLogin(
    email: String,
    password: String,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {
    val db = FirebaseDatabase.getInstance().reference.child("users")
    db.orderByChild("email").equalTo(email)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                var matchedUuid: String? = null
                for (userSnap in snapshot.children) {
                    val dbPassword = userSnap.child("password").getValue(String::class.java)
                    if (dbPassword == password) {
                        found = true
                        matchedUuid = userSnap.key
                        UserSession.uuid = matchedUuid
                        break
                    }
                }
                if (found && matchedUuid != null) onSuccess(matchedUuid) else onFailure()
            }
            override fun onCancelled(error: DatabaseError) {
                onFailure()
            }
        })
}