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
import ui.screens.patient.PatientReportsScreen   // same UI, different data source

/**
 * Doctor-only Prescriptions & Diagnosis viewer/editor.
 * Uses a DIFFERENT Firestore collection: "prescriptions"
 * (implemented inside PatientReportsScreen via overrides).
 */
@Composable
fun DoctorPatientPrescriptionsScreen(
    navController: NavController,
    patientId: String,
    patientName: String,
    // Chat integration parameters (optional)
    fromChat: Boolean = false,
    chatConversationId: String = ""
) {
    val context = LocalContext.current

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

            // 🔹 Override target patient
            patientIdOverride = patientId,

            // 🔹 Doctors CAN upload prescriptions
            canUpload = true,

            // 🔹 Screen title
            title = "Prescriptions – $patientName",

            // 🔹 Tell PatientReportsScreen to use prescriptions collection
            collectionOverride = "prescriptions",
            
            // 🔹 Security: filter by doctor ID and use it when uploading
            doctorIdFilter = doctorId!!,
            
            // 🔹 Chat integration for automatic message on upload
            fromChat = fromChat,
            chatDoctorId = doctorId!!,
            chatPatientId = patientId,
            chatConversationId = chatConversationId
        )
    }
}