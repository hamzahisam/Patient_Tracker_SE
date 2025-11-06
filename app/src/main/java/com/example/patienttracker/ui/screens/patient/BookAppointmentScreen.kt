package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.clickable


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

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Book Appointment",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
            val canBook = availableDays.any { dayName.contains(it.take(3)) || it.contains(dayName.take(3)) }

            Button(
                onClick = {
                    if (canBook) {
                        AppointmentStorage.saveAppointment(
                            context,
                            doctor,
                            selectedDate.format(dateFormatter),
                            timing
                        )
                        message = "Appointment booked successfully!"
                    } else {
                        message = "Doctor not available on this day."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CC7CD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Booking", color = Color.White)
            }

            if (message.isNotEmpty()) {
                Text(message, color = if (message.contains("success")) Color(0xFF2A6C74) else Color.Red)
            }
        }
    }
}

@Composable
fun DatePicker(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
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
                    Text(date.dayOfMonth.toString(), color = if (isSelected) Color.White else Color(0xFF2A6C74))
                    Text(date.dayOfWeek.name.take(3), color = if (isSelected) Color.White else Color(0xFF2A6C74))
                }
            }
        }
    }
}
