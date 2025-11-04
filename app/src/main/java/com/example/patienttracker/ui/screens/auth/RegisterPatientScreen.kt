package com.example.patienttracker.ui.screens.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterPatientScreen(navController: NavController, context: Context) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Patient Registration",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                            phone.isBlank() || password.isBlank() || confirmPassword.isBlank()
                        ) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isUpperCase() }) {
                            Toast.makeText(
                                context,
                                "Password must be at least 8 characters, include a number and uppercase letter",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        isLoading = true
                        try {
                            // 1) Firebase Auth sign-up
                            val auth = Firebase.auth
                            val user = auth.createUserWithEmailAndPassword(email.trim(), password)
                                .await().user ?: throw IllegalStateException("Auth user is null")

                            // 2) Generate next humanId (e.g., 000001) for role = patient
                            val humanId = nextHumanId("patient")

                            // 3) Create profile doc in Firestore
                            createUserProfile(
                                uid = user.uid,
                                role = "patient",
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                humanId = humanId
                            )

                            Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()

                            // 4) Navigate to the Account Created screen that shows the Patient ID and a "Go to Login" button.
                            navController.navigate("account_created/$humanId") {
                                popUpTo("register_patient") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Creating..." else "Create Account")
            }
        }
    }
}

// ------------------ Firestore helpers ------------------

private suspend fun nextHumanId(role: String): String {
    val db = Firebase.firestore
    val ref = db.collection("counters").document(role)
    val next = db.runTransaction { tx ->
        val snap = tx.get(ref)
        val current = snap.getLong("next") ?: 1L
        tx.set(ref, mapOf("next" to (current + 1)), SetOptions.merge())
        current
    }.await()
    return next.toString().padStart(6, '0')
}

private suspend fun createUserProfile(
    uid: String,
    role: String,
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    humanId: String
) {
    val db = Firebase.firestore
    val doc = mapOf(
        "role" to role,
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "humanId" to humanId,
        "createdAt" to Timestamp.now()
    )
    db.collection("users").document(uid).set(doc).await()
}
