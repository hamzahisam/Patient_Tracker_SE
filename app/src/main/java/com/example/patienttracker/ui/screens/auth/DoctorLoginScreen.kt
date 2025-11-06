package com.example.patienttracker.ui.screens.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.patienttracker.ui.screens.auth.model.AppUser

@Composable
fun DoctorLoginScreen(
    navController: NavController,
    context: Context
) {
    var doctorId by remember { mutableStateOf("") }   // humanId like 000001
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var showWelcome by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("") }
    var doctorIdDisplay by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    if (showWelcome) {
        LaunchedEffect(Unit) {
            delay(2000)
            val first = doctorName.substringBefore(' ')
            val last = doctorName.substringAfter(' ', "")
            navController.navigate("doctor_welcome/${first}/${last}/${doctorIdDisplay}")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome Dr. $doctorName",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00796B)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "DoctorID# $doctorIdDisplay",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7FBFB)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Doctor Login",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF005F73)
                )

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = doctorId,
                    onValueChange = { doctorId = it },
                    label = { Text("Doctor ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (doctorId.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Enter Doctor ID and password", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            isLoading = true
                            try {
                                // 1) Look up Firestore profile by humanId
                                val profile = findDoctorByHumanId(doctorId.trim())
                                    ?: throw IllegalArgumentException("No doctor with this ID")

                                // 2) Sign in using the profile's email + provided password
                                val authUser = Firebase.auth
                                    .signInWithEmailAndPassword(profile.email, password)
                                    .await()
                                    .user ?: throw IllegalStateException("Auth failed")

                                // 3) Verify role and fetch canonical profile by UID
                                val full = fetchUserProfile(authUser.uid)
                                    ?: throw IllegalStateException("Profile not found")
                                if (full.role != "doctor") throw IllegalStateException("This account is not a doctor")

                                doctorName = listOf(full.firstName, full.lastName).filter { it.isNotBlank() }.joinToString(" ")
                                doctorIdDisplay = full.humanId

                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                                // Store profile data for persistence
                                navController.currentBackStackEntry?.savedStateHandle?.set("firstName", full.firstName)
                                navController.currentBackStackEntry?.savedStateHandle?.set("lastName", full.lastName)
                                navController.currentBackStackEntry?.savedStateHandle?.set("doctorId", full.humanId)

                                // Navigate directly to doctor home (or keep welcome screen if you prefer)
                                navController.navigate("doctor_home/${full.firstName}/${full.lastName}/${full.humanId}") {
                                    popUpTo("doctor_login") { inclusive = true }
                                }
                                showWelcome = true
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Login failed", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A9396)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Signing In..." else "Log In", color = Color.White, fontSize = 18.sp)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Forgot ID / Password?",
                    color = Color(0xFF0077B6),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Please contact admin.", Toast.LENGTH_SHORT).show()
                    },
                    textAlign = TextAlign.Center
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Color(0xFF0A9396)
                )
            }
        }
    }
}

// ----------------- Firestore helpers -----------------

private suspend fun findDoctorByHumanId(humanId: String): AppUser? {
    val db = Firebase.firestore
    val snap = db.collection("users")
        .whereEqualTo("humanId", humanId)
        .whereEqualTo("role", "doctor")
        .limit(1)
        .get()
        .await()

    val d = snap.documents.firstOrNull() ?: return null
    return AppUser(
        uid = d.id,
        role = d.getString("role") ?: "",
        firstName = d.getString("firstName") ?: "",
        lastName = d.getString("lastName") ?: "",
        email = d.getString("email") ?: "",
        humanId = d.getString("humanId") ?: ""
    )
}

private suspend fun fetchUserProfile(uid: String): AppUser? {
    val db = Firebase.firestore
    val d = db.collection("users").document(uid).get().await()
    if (!d.exists()) return null
    return AppUser(
        uid = uid,
        role = d.getString("role") ?: "",
        firstName = d.getString("firstName") ?: "",
        lastName = d.getString("lastName") ?: "",
        email = d.getString("email") ?: "",
        humanId = d.getString("humanId") ?: ""
    )
}
