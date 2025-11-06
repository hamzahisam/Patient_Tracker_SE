package com.example.patienttracker.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun PatientProfileScreen(
    navController: NavController,
    firstName: String?,
    lastName: String?
) {
    val safeFirstName = firstName ?: "Patient"
    val safeLastName = lastName ?: ""
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        // Sign out from Firebase
                        Firebase.auth.signOut()
                        // Navigate to login screen
                        navController.navigate("role") {
                            popUpTo(0) // Clear entire back stack
                        }
                    }
                ) {
                    Text("Confirm", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FEFF) // Same theme color as other screens
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0EA5B8)
            )

            // Profile Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Circle with initials
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0EA5B8)),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = buildString {
                                if (safeFirstName.isNotBlank()) append(safeFirstName.first().uppercaseChar())
                                if (safeLastName.isNotBlank()) append(safeLastName.first().uppercaseChar())
                            }.ifBlank { "P" }
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column {
                            Text(
                                text = "$safeFirstName $safeLastName", // Use safe variables here
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C3D5A)
                            )
                            Text(
                                text = "Patient",
                                fontSize = 14.sp,
                                color = Color(0xFF6AA8B0)
                            )
                        }
                    }

                    Divider(color = Color(0xFFE5EFF3))

                    // Additional profile information can be added here
                    Text(
                        text = "Account Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2A6C74)
                    )
                    // Add more profile details as needed
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = "Log Out",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }
}