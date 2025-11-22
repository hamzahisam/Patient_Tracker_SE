package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun DoctorWelcomeScreen(
    navController: NavController,
    firstName: String,
    lastName: String,
    doctorId: String
) {
    LaunchedEffect(Unit) {
        delay(2000)

        // Make the name & id available to the next screen
        navController.currentBackStackEntry?.savedStateHandle?.set("firstName", firstName)
        navController.currentBackStackEntry?.savedStateHandle?.set("lastName", lastName)
        navController.currentBackStackEntry?.savedStateHandle?.set("doctorId", doctorId)

        navController.navigate("doctor_home/$firstName/$lastName/$doctorId") {
            popUpTo("doctor_welcome/$firstName/$lastName/$doctorId") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar with initials
            val initials = (firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "") +
                    (lastName.firstOrNull()?.uppercaseChar()?.toString() ?: "")

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E242A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB3E5FC)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Greeting text
            Text(
                text = "Welcome back,",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dr. $firstName $lastName",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CB7C2)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Doctor ID #$doctorId",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Loading your dashboard...",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}
