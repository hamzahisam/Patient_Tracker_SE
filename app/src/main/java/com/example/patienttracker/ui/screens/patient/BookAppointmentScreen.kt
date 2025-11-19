package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentStorage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// 🔹 import your auth layer (adjust package if needed)
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.auth.UserProfile
import com.example.patienttracker.ui.screens.common.BackButton

@Composable
fun BookAppointmentScreen(
    navController: NavController,
    context: Context,
    doctor: DoctorFull
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD)))

    val availableDays = doctor.days.split(",").map { it.trim().lowercase(Locale.ROOT) }
    val timing = doctor.timings
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var message by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Firestore instance (remember so it's not recreated every recomposition)
    val db = remember { FirebaseFirestore.getInstance() }

    // 🔹 Load current patient profile from AuthManager
    var patientProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(Unit) {
        try {
            patientProfile = AuthManager.getCurrentUserProfile()
        } catch (e: Exception) {
            Log.e("BookAppointment", "Failed to load patient profile", e)
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")

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
                        text = "Book Appointment",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dr. ${doctor.firstName} ${doctor.lastName}", fontWeight = FontWeight.Bold)
            Text("Speciality: ${doctor.speciality}", color = Color(0xFF4CB7C2))
            Text("Available Days: ${doctor.days}")
            Text("Timings: ${doctor.timings}")

            Spacer(Modifier.height(12.dp))

            DatePicker(selectedDate) { date ->
                selectedDate = date
            }

            val dayName = selectedDate.dayOfWeek.name.lowercase(Locale.ROOT)
            val canBook = availableDays.any {
                dayName.contains(it.take(3)) || it.contains(dayName.take(3))
            }

            Button(
                onClick = {
                    if (!canBook) {
                        message = "Doctor not available on this day."
                        return@Button
                    }

                    // 🔹 Ensure we actually have a logged-in patient profile
                    val profile = patientProfile
                    val currentPatientId = profile?.humanId?.ifBlank { null } ?: profile?.uid

                    if (currentPatientId.isNullOrBlank()) {
                        message = "Could not find your patient profile. Please log in again."
                        return@Button
                    }

                    val formattedDate = selectedDate.format(dateFormatter)

                    // Optional: still save locally if you want
                    AppointmentStorage.saveAppointment(
                        context,
                        doctor,
                        formattedDate,
                        timing
                    )

                    // 🔹 Save to Firestore
                    isSaving = true
                    message = ""

                    val appointmentData = hashMapOf(
                        // doctor info
                        "doctorId" to doctor.id,
                        "doctorFirstName" to doctor.firstName,
                        "doctorLastName" to doctor.lastName,
                        "doctorSpeciality" to doctor.speciality,

                        // patient info (from profile)
                        "patientId" to currentPatientId,
                        "patientFirstName" to (profile?.firstName ?: ""),
                        "patientLastName" to (profile?.lastName ?: ""),

                        // appointment details
                        "date" to formattedDate,
                        "timing" to timing,
                        "status" to "booked",
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("appointments")
                        .add(appointmentData)
                        .addOnSuccessListener { docRef ->
                            Log.d("BookAppointment", "✅ Appointment stored with id=${docRef.id}")
                            message = "Appointment booked successfully!"
                            isSaving = false
                        }
                        .addOnFailureListener { e ->
                            Log.e("BookAppointment", "❌ Failed to store appointment", e)
                            message = "Failed to book appointment. Please try again."
                            isSaving = false
                        }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CC7CD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isSaving) "Booking…" else "Confirm Booking",
                    color = Color.White
                )
            }

            if (message.isNotEmpty()) {
                Text(
                    message,
                    color = if (message.contains("success", ignoreCase = true))
                        Color(0xFF2A6C74) else Color.Red
                )
            }
        }
    }
}

@Composable
fun DatePicker(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        val today = LocalDate.now()
        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val isSelected = date == selectedDate
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color(0xFF3CC7CD) else Color(0xFFEAF7F8),
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onDateSelected(date) }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        date.dayOfMonth.toString(),
                        color = if (isSelected) Color.White else Color(0xFF2A6C74)
                    )
                    Text(
                        date.dayOfWeek.name.take(3),
                        color = if (isSelected) Color.White else Color(0xFF2A6C74)
                    )
                }
            }
        }
    }
}