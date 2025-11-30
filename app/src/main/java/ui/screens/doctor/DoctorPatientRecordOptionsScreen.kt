package com.example.patienttracker.ui.screens.doctor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import com.example.patienttracker.R
import com.example.patienttracker.ui.screens.doctor.DoctorBottomBar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class DoctorViewHealthSummary(
    val gender: String = "",
    val bloodGroup: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val bloodPressure: String = "",
    val heartRate: String = "",
    val bloodSugar: String = "",
    val lastUpdated: Long = 0L
)

@Composable
fun DoctorPatientRecordOptionsScreen(
    navController: NavController,
    patientId: String,
    patientName: String
) {
    val db = remember { Firebase.firestore }
    var healthSummary by remember { mutableStateOf(DoctorViewHealthSummary()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load patient's health summary from Firestore using humanId
    LaunchedEffect(patientId) {
        try {
            // Find user document by humanId
            val querySnapshot = db.collection("users")
                .whereEqualTo("humanId", patientId)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                healthSummary = DoctorViewHealthSummary(
                    gender = doc.getString("gender") ?: "",
                    bloodGroup = doc.getString("bloodGroup") ?: "",
                    age = doc.getString("age") ?: "",
                    height = doc.getString("height") ?: "",
                    weight = doc.getString("weight") ?: "",
                    bloodPressure = doc.getString("bloodPressure") ?: "",
                    heartRate = doc.getString("heartRate") ?: "",
                    bloodSugar = doc.getString("bloodSugar") ?: "",
                    lastUpdated = doc.getLong("healthSummaryLastUpdated") ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e("DoctorRecords", "Error loading patient health summary", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        // Patients tab is conceptually active here
        bottomBar = { DoctorBottomBar(navController, selectedTab = 2) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF4CB7C2)
                    )
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = if (patientName.isNotBlank()) "Records – $patientName" else "Records",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4CB7C2)
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- Health Summary Card (Read-only for doctor) ---
            DoctorHealthSummaryCard(
                healthSummary = healthSummary,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 1) Prescriptions & Diagnosis – doctor can edit here
            DoctorRecordOptionCard(
                title = "Prescriptions & Diagnosis",
                iconRes = R.drawable.ic_doctor,
                subtitle = "Upload here"
            ) {
                navController.navigate(
                    "doctor_patient_prescriptions_screen/$patientId/$patientName"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 2) Reports – read-only for doctor, separate from prescriptions
            DoctorRecordOptionCard(
                title = "Reports",
                iconRes = R.drawable.ic_record,
                subtitle = null
            ) {
                navController.navigate(
                    "doctor_patient_reports_screen/$patientId/$patientName"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DoctorRecordOptionCard(
    title: String,
    iconRes: Int,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(
                width = 2.dp,
                color = Color(0xFF4CB7C2),
                shape = RoundedCornerShape(24.dp)
            )
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        .size(56.dp)
                        .padding(bottom = 12.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CB7C2)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorHealthSummaryCard(
    healthSummary: DoctorViewHealthSummary,
    isLoading: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val lastUpdatedText = if (healthSummary.lastUpdated > 0) {
        "Last updated: ${dateFormat.format(Date(healthSummary.lastUpdated))}"
    } else {
        "Not yet updated"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color(0xFF4CB7C2),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF4CB7C2).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health",
                        tint = Color(0xFF4CB7C2),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Health Summary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CB7C2))
                }
            } else {
                // Row 1: Gender & Blood Type
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DoctorHealthStatRow(
                        label = "Gender:",
                        value = healthSummary.gender.ifBlank { "Not set" },
                        modifier = Modifier.weight(1f)
                    )
                    DoctorHealthStatRow(
                        label = "Blood Type:",
                        value = healthSummary.bloodGroup.ifBlank { "Not set" },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF4CB7C2).copy(alpha = 0.2f))

                // Row 2: Age & Weight
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DoctorHealthStatRow(
                        label = "Age:",
                        value = if (healthSummary.age.isNotBlank()) "${healthSummary.age} Years" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                    DoctorHealthStatRow(
                        label = "Weight:",
                        value = if (healthSummary.weight.isNotBlank()) "${healthSummary.weight} Kg" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF4CB7C2).copy(alpha = 0.2f))

                // Row 3: Height & BP
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DoctorHealthStatRow(
                        label = "Height:",
                        value = if (healthSummary.height.isNotBlank()) "${healthSummary.height} cm" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                    DoctorHealthStatRow(
                        label = "BP:",
                        value = if (healthSummary.bloodPressure.isNotBlank()) "${healthSummary.bloodPressure}" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF4CB7C2).copy(alpha = 0.2f))

                // Row 4: Heart Rate & Blood Sugar
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DoctorHealthStatRow(
                        label = "Heart Rate:",
                        value = if (healthSummary.heartRate.isNotBlank()) "${healthSummary.heartRate} bpm" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                    DoctorHealthStatRow(
                        label = "Sugar:",
                        value = if (healthSummary.bloodSugar.isNotBlank()) "${healthSummary.bloodSugar} mg/dL" else "Not set",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Last Updated Badge at bottom left
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF4CB7C2).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = lastUpdatedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CB7C2),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorHealthStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF4CB7C2)
        )
    }
}