package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay


@Composable
fun PatientWelcomeScreen(
    navController: NavController,
    firstName: String,
    lastName: String,
    patientId: String
) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds delay before moving ahead
        navController.navigate("patient_home") {
            popUpTo("patient_welcome/$firstName/$lastName/$patientId") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome, $firstName $lastName!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Patient ID: #$patientId",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
