package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.patienttracker.auth.AuthManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.example.patienttracker.ui.screens.patient.PatientBottomBar

data class RecentDoctor(
    val doctorId: String,
    val doctorName: String,
    val speciality: String,
    val lastVisitDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDoctorsScreen(
    navController: NavController,
    context: Context
) {
    val accent = Color(0xFF4CB7C2)
    var recentDoctors by remember { mutableStateOf<List<RecentDoctor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load recent doctors from past appointments
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            val patientId = profile?.humanId

            if (patientId.isNullOrBlank()) {
                error = "Could not load patient profile"
                isLoading = false
                return@LaunchedEffect
            }

            val db = Firebase.firestore
            val snapshot = db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("status", "booked")
                .get()
                .await()

            // Group by doctor and get unique doctors with their last visit
            val doctorMap = mutableMapOf<String, RecentDoctor>()
            
            for (doc in snapshot.documents) {
                val doctorId = doc.getString("doctorId") ?: continue
                val doctorFirstName = doc.getString("doctorFirstName") ?: ""
                val doctorLastName = doc.getString("doctorLastName") ?: ""
                val doctorName = "Dr. $doctorFirstName $doctorLastName".trim()
                val speciality = doc.getString("doctorSpeciality") ?: doc.getString("speciality") ?: "Specialist"
                val date = doc.getString("date") ?: ""

                // Keep the most recent appointment for each doctor
                if (!doctorMap.containsKey(doctorId)) {
                    doctorMap[doctorId] = RecentDoctor(
                        doctorId = doctorId,
                        doctorName = doctorName.ifBlank { "Doctor" },
                        speciality = speciality,
                        lastVisitDate = date
                    )
                }
            }

            recentDoctors = doctorMap.values.toList()
            isLoading = false
        } catch (e: Exception) {
            error = e.message ?: "Failed to load recent doctors"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recent Doctors",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = { PatientBottomBar(navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accent
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "An error occurred",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                recentDoctors.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recent doctors",
                            color = accent,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Book an appointment to see your doctors here",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(recentDoctors) { doctor ->
                            RecentDoctorCard(
                                doctor = doctor,
                                onClick = {
                                    // Navigate to doctor list filtered by this doctor's specialty
                                    // or directly to book appointment if we have full doctor data
                                    navController.navigate("doctor_list/${doctor.speciality}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDoctorCard(
    doctor: RecentDoctor,
    onClick: () -> Unit
) {
    val accent = Color(0xFF4CB7C2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.1f))
                    .border(1.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initials = doctor.doctorName
                    .replace("Dr. ", "")
                    .split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .ifBlank { "D" }

                Text(
                    text = initials,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.doctorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = doctor.speciality,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
                if (doctor.lastVisitDate.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Last visit: ${doctor.lastVisitDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
