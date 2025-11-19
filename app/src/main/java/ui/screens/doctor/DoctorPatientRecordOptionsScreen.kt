package com.example.patienttracker.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import com.example.patienttracker.R
import com.example.patienttracker.ui.screens.doctor.DoctorBottomBar

@Composable
fun DoctorPatientRecordOptionsScreen(
    navController: NavController,
    patientId: String,
    patientName: String
) {
    // 🔹 Separate gradients for the two cards
    val gradientPrescriptions = Brush.linearGradient(
        listOf(Color(0xFF6DD5FA), Color(0xFF2980B9))
    )
    val gradientReports = Brush.linearGradient(
        listOf(Color(0xFFB2EBF2), Color(0xFF0097A7))
    )

    Scaffold(
        // Patients tab is conceptually active here
        bottomBar = { DoctorBottomBar(navController, selectedTab = 2) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FC))
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = patientName.ifBlank { "Patient $patientId" },
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1C3D5A),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 🔹 1) Prescriptions & Diagnosis – doctor can edit here
            DoctorRecordOptionCard(
                title = "Prescriptions & Diagnosis",
                iconRes = R.drawable.ic_doctor,   // or a separate icon if you have one
                gradient = gradientPrescriptions
            ) {
                navController.navigate(
                    "doctor_patient_prescriptions_screen/$patientId/$patientName"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🔹 2) Reports – read-only for doctor, separate from prescriptions
            DoctorRecordOptionCard(
                title = "Reports",
                iconRes = R.drawable.ic_records,
                gradient = gradientReports
            ) {
                navController.navigate(
                    "doctor_patient_reports_screen/$patientId/$patientName"
                )
            }
        }
    }
}

@Composable
private fun DoctorRecordOptionCard(
    title: String,
    iconRes: Int,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                )
            }
        }
    }
}