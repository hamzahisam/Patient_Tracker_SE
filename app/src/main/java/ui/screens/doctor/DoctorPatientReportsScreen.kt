package com.example.patienttracker.ui.screens.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import ui.screens.patient.PatientReportsScreen
import java.net.URLDecoder

@Composable
fun DoctorPatientReportsScreen(
    navController: NavController,
    patientId: String,
    patientName: String
) {
    val context = LocalContext.current
    val decodedPatientName = URLDecoder.decode(patientName, "UTF-8")
    
    // Get logged-in doctor's ID for security filtering
    var doctorId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val profile = AuthManager.getCurrentUserProfile()
        doctorId = profile?.humanId ?: ""
        isLoading = false
    }
    
    // Wait for doctor ID to load before showing records
    if (isLoading || doctorId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4CB7C2))
        }
    } else {
        PatientReportsScreen(
            navController = navController,
            context = context,
            patientIdOverride = patientId,
            canUpload = false,
            title = "Reports – $decodedPatientName",
            doctorIdFilter = doctorId!!  // Security: only show records shared with this doctor
        )
    }
}