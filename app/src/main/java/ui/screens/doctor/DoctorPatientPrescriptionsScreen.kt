package com.example.patienttracker.ui.screens.doctor

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
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
    patientName: String
) {
    val context = LocalContext.current

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
        collectionOverride = "prescriptions"
    )
}