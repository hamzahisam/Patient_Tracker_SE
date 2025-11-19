package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.patienttracker.ui.screens.common.BackButton
import kotlinx.parcelize.Parcelize

@Parcelize
data class DoctorFull(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val speciality: String,
    val days: String,      // human-readable, e.g. "Mon, Wed, Fri"
    val timings: String    // human-readable, e.g. "6:00 pm – 9:00 pm"
) : Parcelable

@Composable
fun DoctorListScreen(
    navController: NavController,
    context: Context,
    specialityFilter: String?
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD)))

    val db = Firebase.firestore
    var doctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(specialityFilter) {
        loading = true

        // ✅ Only filter by role in Firestore
        val query: Query = db.collection("users")
            .whereEqualTo("role", "doctor")

        query.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("humanId")
                        ?: doc.getString("doctorId")
                        ?: return@mapNotNull null

                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val speciality = doc.getString("speciality") ?: ""

                    val daysList = (doc.get("days") as? List<*>)?.mapNotNull { it as? String }
                        ?: emptyList()
                    val daysDisplay = if (daysList.isNotEmpty()) {
                        daysList.joinToString(", ")
                    } else {
                        ""
                    }

                    val timingsRaw = (doc.get("timings") as? List<*>)?.mapNotNull {
                        when (it) {
                            is Long -> it.toInt()
                            is Int -> it
                            else -> null
                        }
                    } ?: emptyList()

                    val timingsDisplay = if (timingsRaw.size >= 2) {
                        "${formatTimeHHmm(timingsRaw[0])} – ${formatTimeHHmm(timingsRaw[1])}"
                    } else {
                        ""
                    }

                    DoctorFull(
                        id = id,
                        firstName = first,
                        lastName = last,
                        email = email,
                        phone = phone,
                        speciality = speciality,
                        days = daysDisplay,
                        timings = timingsDisplay
                    )
                }
                doctors = list
                loading = false
            }
            .addOnFailureListener {
                doctors = emptyList()
                loading = false
            }
    }

    val filtered =
        if (specialityFilter.isNullOrBlank() || specialityFilter == "All") {
            doctors
        } else {
            doctors.filter { it.speciality.contains(specialityFilter, ignoreCase = true) }
        }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Back Button
                    BackButton(
                        navController = navController,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    // Title
                    Text(
                        text = specialityFilter?.ifBlank { "All Doctors" } ?: "All Doctors",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        bottomBar = { PatientBottomBar(navController) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color(0xFFF6F8FC)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "No doctors available",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(filtered) { doc ->
                    DoctorCard(doc) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedDoctor", doc)

                        navController.navigate("book_appointment")
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorCard(
    doctor: DoctorFull,
    onBookClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1C3D5A)
            )
            Text(
                text = doctor.speciality,
                color = Color(0xFF4CB7C2),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            if (doctor.days.isNotBlank()) {
                Text("Days: ${doctor.days}", color = Color(0xFF2A6C74))
            }
            if (doctor.timings.isNotBlank()) {
                Text("Timings: ${doctor.timings}", color = Color(0xFF2A6C74))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBookClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CC7CD)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Book Appointment", color = Color.White)
            }
        }
    }
}

/**
 * Convert 1800 -> "6:00 pm", 900 -> "9:00 am"
 * (assuming HHmm integer representation)
 */
private fun formatTimeHHmm(value: Int): String {
    val hours24 = value / 100
    val minutes = value % 100

    val amPm = if (hours24 >= 12) "pm" else "am"
    val hours12 = when {
        hours24 == 0 -> 12
        hours24 > 12 -> hours24 - 12
        else -> hours24
    }

    return String.format("%d:%02d %s", hours12, minutes, amPm)
}