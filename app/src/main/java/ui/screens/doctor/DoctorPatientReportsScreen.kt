package com.example.patienttracker.ui.screens.doctor

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import ui.screens.patient.PatientReportsScreen

@Composable
fun DoctorPatientReportsScreen(
    navController: NavController,
    patientId: String,
    patientName: String
) {
    val context = LocalContext.current
    PatientReportsScreen(
        navController = navController,
        context = context,
        patientIdOverride = patientId,
        canUpload = false,
        title = "Records – $patientName"
    )
}