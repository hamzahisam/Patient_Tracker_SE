package com.example.patienttracker.ui.screens.auth

import android.content.Context
import android.widget.Toast
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.patienttracker.ui.screens.auth.model.AppUser

@Composable
fun PatientLoginScreen(
    navController: NavController,
    context: Context,
    onForgotPassword: () -> Unit = {}
) {
    var idOrEmail by remember { mutableStateOf("") }     // accepts PatientID or email
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FEFF)
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
                    text = "Patient Login",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0EA5B8)
                )

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = idOrEmail,
                    onValueChange = { idOrEmail = it },
                    label = { Text("Patient ID or Email") },
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
                            isLoading = true
                            try {
                                // 1) Resolve email if user typed a humanId (e.g., 000001), else use the email directly
                                val emailToUse = if (idOrEmail.contains("@")) {
                                    idOrEmail.trim()
                                } else {
                                    // treat as humanId
                                    val u = findPatientByHumanId(idOrEmail.trim())
                                        ?: throw IllegalArgumentException("No patient with this ID")
                                    u.email
                                }

                                // 2) Firebase Auth sign-in
                                val user = Firebase.auth
                                    .signInWithEmailAndPassword(emailToUse, password)
                                    .await()
                                    .user ?: throw IllegalStateException("Auth failed")

                                // 3) Fetch profile from Firestore
                                val profile = fetchUserProfile(user.uid)
                                    ?: throw IllegalStateException("Profile not found")

                                if (profile.role != "patient") {
                                    throw IllegalStateException("This account is not a patient")
                                }

                                // In PatientLoginScreen.kt, find this section and update:
                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                                // In PatientLoginScreen.kt, update the login success section:
                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                                // Store profile data for persistence
                                navController.currentBackStackEntry?.savedStateHandle?.set("firstName", profile.firstName)
                                navController.currentBackStackEntry?.savedStateHandle?.set("lastName", profile.lastName)

                                // Navigate directly to patient home
                                navController.navigate("patient_home/${profile.firstName}/${profile.lastName}") {
                                    popUpTo("patient_login") { inclusive = true }
                                }

                                // 4) Hand off name/ID to the next screen via savedStateHandle
                                // Replace this section in the login button onClick:
                                navController.currentBackStackEntry?.savedStateHandle?.set("firstName", profile.firstName)
                                navController.currentBackStackEntry?.savedStateHandle?.set("lastName", profile.lastName)

                                // Navigate to patient_home with arguments
                                navController.navigate("patient_home/${profile.firstName}/${profile.lastName}") {
                                    popUpTo("patient_login") { inclusive = true }
                                }
                                // If you instead want a welcome screen, use:
                                // navController.navigate("patient_welcome/${profile.firstName}/${profile.lastName}/${profile.humanId}")

                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Invalid ID/email or password", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && idOrEmail.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5B8)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (isLoading) "Signing In..." else "Log In", color = Color.White, fontSize = 18.sp)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Forgot ID / Password?",
                    color = Color(0xFF0077B6),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onForgotPassword() },
                    textAlign = TextAlign.Center
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Color(0xFF0EA5B8)
                )
            }
        }
    }
}

/* ------------ Helpers (Firestore) ------------ */

private suspend fun findPatientByHumanId(humanId: String): AppUser? {
    val db = Firebase.firestore
    val snap = db.collection("users")
        .whereEqualTo("humanId", humanId)
        .whereEqualTo("role", "patient")
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