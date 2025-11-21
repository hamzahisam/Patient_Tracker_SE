package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentStorage
import com.example.patienttracker.ui.screens.patient.PatientBottomBar
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
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(
                        navController = navController,
                        modifier = Modifier
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Book Appointment",
                        color = Color(0xFF4CB7C2),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        bottomBar = {
            PatientBottomBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
fun DatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val totalDaysToShow = 20   // e.g. next 20 days

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(totalDaysToShow) { index ->
            val date = today.plusDays(index.toLong())
            val isSelected = date == selectedDate

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF3CC7CD) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(0xFF4CB7C2)),
                shadowElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onDateSelected(date) }
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // day number
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    // short day name (MON, TUE…)
                    Text(
                        text = date.dayOfWeek.name.take(3),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}