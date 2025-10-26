package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .background(Color(0xFFE0F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome Dr. $firstName $lastName",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00796B)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DoctorID# $doctorId",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}
