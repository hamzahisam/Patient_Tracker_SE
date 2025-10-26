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

    val doctors = remember { readDoctorCsv(context) }
    val filtered = remember(specialityFilter) {
        if (specialityFilter.isNullOrBlank() || specialityFilter == "All")
            doctors
        else doctors.filter { it.speciality.equals(specialityFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = specialityFilter?.ifBlank { "All Doctors" } ?: "All Doctors",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
            items(filtered) { doc ->
                DoctorCard(doc) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedDoctor", doc)
                    navController.navigate("book_appointment")
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

fun readDoctorCsv(context: Context): List<DoctorFull> {
    return try {
        val inputStream = context.assets.open("DoctorAccounts.csv")
        val lines = inputStream.bufferedReader().readLines().drop(1)

        lines.mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 9) {
                // handle cases where Days or Timings fields contain commas
                val id = parts[0].trim()
                val firstName = parts[1].trim()
                val lastName = parts[2].trim()
                val email = parts[3].trim()
                val phone = parts[4].trim()
                val speciality = parts[6].trim()

                // everything between index 7 and the second-last column = Days
                val days = parts.subList(7, parts.size - 1).joinToString(", ") { it.trim() }

                // last column = timings
                val timings = parts.last().trim()

                DoctorFull(
                    id = id,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    speciality = speciality,
                    days = days,
                    timings = timings
                )
            } else null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
