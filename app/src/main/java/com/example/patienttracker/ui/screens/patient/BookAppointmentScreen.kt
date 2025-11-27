package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentStorage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// 🔹 import your auth layer
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.auth.UserProfile
import com.example.patienttracker.ui.screens.common.BackButton

@Composable
fun BookAppointmentScreen(
    navController: NavController,
    context: Context,
    doctor: DoctorFull
) {
    val availableDays = remember(doctor.days) {
        doctor.days.split(",").map { it.trim().lowercase(Locale.ROOT) }
    }
    val timingStrings = remember(doctor.timings) {
        doctor.timings.split("–", "-").map { it.trim() }
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var bookedSlots by remember { mutableStateOf(listOf<String>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val db = remember { FirebaseFirestore.getInstance() }

    // 🔹 Load patient profile
    var patientProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            patientProfile = profile
        } catch (e: Exception) {
            Log.e("BookAppointment", "Failed to load patient profile", e)
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
    val slotFormatter = DateTimeFormatter.ofPattern("h:mm a")

    // Real-time Firestore listener for booked slots
    DisposableEffect(selectedDate) {
        val formattedDate = selectedDate.format(dateFormatter)

        val listenerRegistration = db.collection("appointments")
            .whereEqualTo("doctorId", doctor.id)
            .whereEqualTo("date", formattedDate)
            .whereEqualTo("status", "booked")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BookAppointment", "Real-time listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    bookedSlots = snapshot.documents.mapNotNull { it.getString("timing") }
                    Log.d("BookAppointment", "Realtime updated slots = $bookedSlots")
                }
            }

        // Runs when `selectedDate` changes OR screen disposes
        onDispose {
            listenerRegistration.remove()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(navController = navController)
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
            PatientBottomBar(navController)
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Doctor header
            item {
                Text("Dr. ${doctor.firstName} ${doctor.lastName}", fontWeight = FontWeight.Bold)
                Text("Speciality: ${doctor.speciality}", color = Color(0xFF4CB7C2))
                Text("Available Days: ${doctor.days}")
                Text("Timings: ${doctor.timings}")

                Spacer(Modifier.height(12.dp))
            }

            // Date picker
            item {
                DatePicker(selectedDate, availableDays) { date ->
                    selectedDate = date
                    selectedTime = null
                }
            }

            // Time slots + confirm button or not-available message
            item {
                val dayName = selectedDate.dayOfWeek.name.lowercase(Locale.ROOT)
                val canBook = availableDays.any { dayName.contains(it.take(3)) || it.contains(dayName.take(3)) }

                if (!canBook) {
                    Text(
                        "Doctor not available on this day.",
                        color = Color.Red
                    )
                } else {
                    // --- Time slot picker ---
                    val slots = remember(selectedDate) { generateTimeSlots(timingStrings) }
                    var selectedTimeState by remember { mutableStateOf(selectedTime) }

                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Select Time Slot:", fontWeight = FontWeight.Bold)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = true
                        ) {
                            items(slots) { slot ->
                                val isSelected = slot == selectedTimeState
                                val isBooked = bookedSlots.contains(slot)
                                // treat time slots that are already in the past (for the selected date) as disabled
                                val isPast = isSlotInPast(selectedDate, slot)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isSelected -> Color(0xFF3CC7CD)
                                        isBooked || isPast -> Color.Red.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF4CB7C2)),
                                    shadowElevation = if (isSelected) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .clickable(enabled = !isBooked && !isPast) {
                                            selectedTimeState = slot
                                            selectedTime = slot
                                        }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        slot,
                                        modifier = Modifier.padding(8.dp),
                                        color = when {
                                            isSelected -> Color.White
                                            isBooked || isPast -> Color.White
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Confirm button ---
                        Button(
                            onClick = {
                                if (!canBook) return@Button

                                val profile = patientProfile
                                val currentPatientId = profile?.humanId?.ifBlank { null } ?: profile?.uid
                                if (currentPatientId.isNullOrBlank() || selectedTime == null) {
                                    message = "Select a valid date and time."
                                    return@Button
                                }

                                // Clear any old message and show confirmation dialog
                                message = ""
                                showConfirmDialog = true
                            },
                            enabled = selectedTime != null && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CC7CD)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isSaving) "Booking…" else "Confirm Booking", color = Color.White)
                        }
                    }
                }
            }

            // Message at the bottom
            item {
                if (message.isNotEmpty()) {
                    Text(
                        message,
                        color = if (message.contains("success", ignoreCase = true)) Color(0xFF2A6C74) else Color.Red
                    )
                }
            }
        }
    }

    // Confirmation dialog for booking appointment
    if (showConfirmDialog) {
        val profile = patientProfile
        val currentPatientId = profile?.humanId?.ifBlank { null } ?: profile?.uid
        val selectedTiming = selectedTime

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Appointment") },
            text = {
                Text(
                    text = "Do you want to book an appointment with " +
                            "Dr. ${doctor.firstName} ${doctor.lastName} on " +
                            selectedDate.format(dateFormatter) +
                            (if (selectedTiming != null) " at $selectedTiming?" else "?")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentPatientId.isNullOrBlank() || selectedTiming == null) {
                            message = "Select a valid date and time."
                            showConfirmDialog = false
                            return@TextButton
                        }

                        val formattedDate = selectedDate.format(dateFormatter)

                        // Start saving and clear old messages
                        isSaving = true
                        message = ""
                        showConfirmDialog = false

                        // First, check if this patient already has an ACTIVE appointment
                        // with this same doctor on this date (status = "booked" and time not in the past)
                        db.collection("appointments")
                            .whereEqualTo("patientId", currentPatientId)
                            .whereEqualTo("doctorId", doctor.id)
                            .whereEqualTo("date", formattedDate)
                            .whereEqualTo("status", "booked")
                            .get()
                            .addOnSuccessListener { sameDoctorSnapshot ->
                                val hasActiveSameDoctor = sameDoctorSnapshot.documents.any { doc ->
                                    val timingStr = doc.getString("timing") ?: return@any false
                                    // if the stored slot time is still in the future, treat it as active
                                    !isSlotInPast(selectedDate, timingStr)
                                }

                                if (hasActiveSameDoctor) {
                                    // Patient already has an active appointment with this doctor on this date
                                    message = "You already have an active appointment with this doctor on this day."
                                    isSaving = false
                                } else {
                                    // Next, check if this patient already has an appointment at this exact time (any doctor)
                                    db.collection("appointments")
                                        .whereEqualTo("patientId", currentPatientId)
                                        .whereEqualTo("date", formattedDate)
                                        .whereEqualTo("timing", selectedTiming)
                                        .whereEqualTo("status", "booked")
                                        .get()
                                        .addOnSuccessListener { existingSnapshot ->
                                            if (!existingSnapshot.isEmpty) {
                                                // Patient already has an appointment at this time (with any doctor)
                                                message = "You already have an appointment at this time."
                                                isSaving = false
                                            } else {
                                                // Safe to book (allow multiple appointments per day as long as times don't conflict,
                                                // and only 1 active appointment per doctor per day)
                                                val appointmentData = hashMapOf(
                                                    "doctorId" to doctor.id,
                                                    "doctorFirstName" to doctor.firstName,
                                                    "doctorLastName" to doctor.lastName,
                                                    "doctorSpeciality" to doctor.speciality,
                                                    "patientId" to currentPatientId,
                                                    "patientFirstName" to (profile?.firstName ?: ""),
                                                    "patientLastName" to (profile?.lastName ?: ""),
                                                    "date" to formattedDate,
                                                    "timing" to selectedTiming,
                                                    "status" to "booked",
                                                    "createdAt" to FieldValue.serverTimestamp()
                                                )

                                                // Save locally only if Firestore booking is allowed
                                                AppointmentStorage.saveAppointment(
                                                    context,
                                                    doctor,
                                                    formattedDate,
                                                    selectedTiming
                                                )

                                                db.collection("appointments")
                                                    .add(appointmentData)
                                                    .addOnSuccessListener { docRef ->
                                                        Log.d("BookAppointment", "Appointment stored with id=${docRef.id}")
                                                        message = "Appointment booked successfully!"
                                                        isSaving = false
                                                        selectedTime = null
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("BookAppointment", "Failed to store appointment", e)
                                                        message = "Failed to book appointment. Please try again."
                                                        isSaving = false
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("BookAppointment", "Failed to verify existing appointment time", e)
                                            message = "Failed to verify existing appointments. Please try again."
                                            isSaving = false
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("BookAppointment", "Failed to verify existing same-doctor appointments", e)
                                message = "Failed to verify existing appointments. Please try again."
                                isSaving = false
                            }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------
// Generate 15-min slots between doctor timings
fun generateTimeSlots(timings: List<String>): List<String> {
    val slots = mutableListOf<String>()
    if (timings.size < 2) return slots

    val displayFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun parseFlexible(timeStr: String): LocalTime? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            LocalTime.parse(timeStr.trim().uppercase(), formatter)
        } catch (e: Exception) {
            try {
                val num = timeStr.trim().toInt()
                if (num < 100) null
                else {
                    val hour = num / 100
                    val minute = num % 100
                    LocalTime.of(hour, minute)
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    val startTime = parseFlexible(timings[0])
    val endTime = parseFlexible(timings[1])

    if (startTime != null && endTime != null) {
        var time: LocalTime = startTime
        while (time.isBefore(endTime)) {
            slots.add(time.format(displayFormatter))
            time = time.plusMinutes(15)
        }
    }

    return slots
}

private fun isSlotInPast(selectedDate: LocalDate, slot: String): Boolean {
    return try {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val slotTime = LocalTime.parse(slot.trim().uppercase(Locale.US), formatter)
        val slotDateTime = LocalDateTime.of(selectedDate, slotTime)
        slotDateTime.isBefore(LocalDateTime.now())
    } catch (e: Exception) {
        false
    }
}

@Composable
fun DatePicker(
    selectedDate: LocalDate,
    availableDays: List<String>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val totalDaysToShow = 20

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(totalDaysToShow) { index ->
            val date = today.plusDays(index.toLong())
            val dayName = date.dayOfWeek.name.lowercase(Locale.ROOT)
            val isAvailable = availableDays.any { dayName.contains(it.take(3)) || it.contains(dayName.take(3)) }
            val isSelected = date == selectedDate

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    isSelected -> Color(0xFF3CC7CD)
                    !isAvailable -> Color.Red.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(1.dp, Color(0xFF4CB7C2)),
                shadowElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .width(72.dp)
                    .clickable(enabled = isAvailable) { onDateSelected(date) }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
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
