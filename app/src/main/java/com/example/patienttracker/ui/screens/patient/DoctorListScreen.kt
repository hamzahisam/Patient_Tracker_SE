package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.data.DoctorAccountStorage
import com.example.patienttracker.data.DoctorAccount
import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.BookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.FullScheduleScreen
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.Query
import com.example.patienttracker.ui.screens.common.BackButton


@Parcelize
data class DoctorFull(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val speciality: String,
    val days: String,
    val timings: String
) : Parcelable

@Composable
fun DoctorListScreen(navController: NavController, context: Context, specialityFilter: String?) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD)))

    val db = Firebase.firestore
    var doctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(specialityFilter) {
        loading = true

        // base query: only doctors from `users` collection
        var query: Query = db.collection("users")
            .whereEqualTo("role", "doctor")

        // if a specific speciality is requested (not "All"), filter by it
        val filter = specialityFilter ?: "All"
        if (filter != "All") {
            query = query.whereEqualTo("speciality", filter)   // or "specialty" if that's your field
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    // in Firestore your id field is `humanId` (fallback to doctorId if present)
                    val id = doc.getString("humanId") ?: doc.getString("doctorId") ?: return@mapNotNull null
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val speciality = doc.getString("speciality") ?: ""
                    val days = doc.getString("days") ?: ""
                    val timings = doc.getString("timings") ?: ""
                    DoctorFull(id, first, last, email, phone, speciality, days, timings)
                }
                doctors = list
                loading = false
            }
            .addOnFailureListener {
                doctors = emptyList()
                loading = false
            }
    }

    val filtered = if (specialityFilter.isNullOrBlank() || specialityFilter == "All") doctors else doctors.filter { it.speciality.equals(specialityFilter, true) }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // --- Back Button ---
                    BackButton(
                        navController = navController,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    // --- Title ---
                    Text(
                        text = specialityFilter?.ifBlank { "All Doctors" } ?: "All Doctors",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
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
                item { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (filtered.isEmpty()) {
                item { Text("No doctors available", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            } else {
                items(filtered) { doc ->
                    DoctorCard(doc) {
                        navController.currentBackStackEntry?.savedStateHandle?.set("selectedDoctor", doc)
                        navController.navigate("book_appointment")
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorCard(doctor: DoctorFull, onBookClick: () -> Unit) {
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
            Text("Days: ${doctor.days}", color = Color(0xFF2A6C74))
            Text("Timings: ${doctor.timings}", color = Color(0xFF2A6C74))
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
