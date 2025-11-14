package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "ChatSelectionScreen"

data class DoctorItem(
    val uid: String,
    val humanId: String,
    val firstName: String,
    val lastName: String,
    val specialty: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSelectionScreen(
    navController: NavController,
    context: Context
) {
    var doctors by remember { mutableStateOf<List<DoctorItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // --- Firestore load ---
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            val patientId = profile?.humanId

            if (patientId.isNullOrBlank()) {
                Log.w(TAG, "No patientId in profile, profile=$profile")
                error = "Not logged in"
                loading = false
                return@LaunchedEffect
            }

            Log.d(TAG, "Loading chat doctors for patientId=$patientId")

            val db = FirebaseFirestore.getInstance()

            // 🔹 Let Firestore do both filters: status AND this patient
            db.collection("appointments")
                .whereEqualTo("status", "booked")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener { snap ->
                    Log.d(TAG, "Appointments query success: size=${snap.size()} for patientId=$patientId")

                    val map = linkedMapOf<String, DoctorItem>()

                    for (doc in snap.documents) {
                        val doctorId = doc.getString("doctorId")
                        val first = doc.getString("doctorFirstName") ?: ""
                        val last = doc.getString("doctorLastName") ?: ""
                        // field name in your screenshot is "doctorSpeciality"
                        val specialty = doc.getString("doctorSpeciality") ?: ""

                        Log.d(
                            TAG,
                            "Doc ${doc.id} -> doctorId=$doctorId, doctorFirstName=$first, doctorLastName=$last, specialty=$specialty"
                        )

                        if (doctorId.isNullOrBlank()) continue

                        // use doctorId as both uid + humanId for now
                        map[doctorId] = DoctorItem(
                            uid = doctorId,
                            humanId = doctorId,
                            firstName = first,
                            lastName = last,
                            specialty = specialty
                        )
                    }

                    doctors = map.values.toList()
                    loading = false
                    Log.d(TAG, "Loaded ${doctors.size} chat doctors for patient $patientId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load chats", e)
                    error = e.message ?: "Failed to load chats"
                    loading = false
                }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading chats", e)
            error = e.message ?: "Unexpected error"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Chats") }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color(0xFFF7FBFF))
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                doctors.isEmpty() -> {
                    Text(
                        text = "No patient chats available yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(doctors) { doctor ->
                            DoctorRow(doctor) {
                                // navigate to chat_screen with doctorId (same as humanId here)
                                navController.navigate("chat_patient/${doctor.humanId}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorRow(
    doctor: DoctorItem,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1C3D5A)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = doctor.specialty,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0EA5B8)
            )
            if (doctor.humanId.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "ID: ${doctor.humanId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}