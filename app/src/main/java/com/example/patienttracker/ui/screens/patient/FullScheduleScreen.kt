package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.data.AppointmentStorage
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.ui.screens.common.BackButton
import kotlinx.coroutines.launch
import com.example.patienttracker.data.firebase.UserRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FullScheduleScreen(navController: NavController, context: Context) {
    val appointments = remember { mutableStateListOf<Appointment>() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }   // <- NEW

    LaunchedEffect(Unit) {
        try {
            val firebaseUid = AuthManager.getCurrentUserId() ?: run {
                isLoading = false
                return@LaunchedEffect
            }

            // Get full user profile
            val profile = AuthManager.getCurrentUserProfile()
            val humanId = profile?.humanId ?: run {
                isLoading = false
                return@LaunchedEffect
            }

            // Fetch appointments based on humanId
            val remoteAppointments = UserRepository.getAppointmentsForPatient(humanId)

            appointments.clear()
            appointments.addAll(remoteAppointments)
        } finally {
            isLoading = false          // <- stop loading in all cases
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
                        text = "My Schedule",
                        color = Color(0xFF4CB7C2),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                  TextButton(
                      onClick = { navController.navigate("patient_past_schedule") },
                      modifier = Modifier
                          .border(
                              width = 1.dp,
                              color = Color(0xFF4CB7C2),
                              shape = RoundedCornerShape(36.dp)
                          )
                          .padding(horizontal = 6.dp, vertical = 0.1.dp)
                  ) {
                      Text(
                          text = "View Past",
                          color = Color(0xFF4CB7C2),
                          style = MaterialTheme.typography.bodyMedium.copy(
                              fontWeight = FontWeight.SemiBold
                          )
                      )
                  }
                }
            }
        },
        bottomBar = { PatientBottomBar(navController) }
    ) { inner ->

        when {
            // 1) While loading → circular progress
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4CB7C2)
                    )
                }
            }

            // 2) Finished loading, but no appointments
            appointments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No appointments scheduled",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3) Appointments found
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(appointments) { app ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    app.date,
                                    color = Color(0xFF4CB7C2),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = app.timing,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = app.doctorFirstName + " " + app.doctorLastName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = app.doctorSpeciality,
                                    color = Color(0xFF4CB7C2)
                                )

                                Spacer(Modifier.height(12.dp))

                                // Decide if this appointment can still be cancelled
                                val canCancel = canCancelAppointment(app)

                                Button(
                                    onClick = {
                                        if (canCancel) {
                                            scope.launch {
                                                UserRepository.cancelAppointment(app)
                                                appointments.remove(app)
                                            }
                                        }
                                    },
                                    enabled = canCancel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canCancel) Color(0xFFFF4B4B) else Color(0xFF555555),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF555555),
                                        disabledContentColor = Color.White
                                    ),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(text = "Cancel appointment")
                                }

                                if (!canCancel) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Appointment cannot be cancelled if less than 24 hours left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF8A80),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun canCancelAppointment(app: Appointment): Boolean {
    return try {
        // Example date: "Wednesday, 26 Nov 2025"
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH)
        // Example time: "9:15 AM"
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

        val localDate = LocalDate.parse(app.date, dateFormatter)
        val localTime = LocalTime.parse(app.timing, timeFormatter)

        val zone = ZoneId.systemDefault()
        val appointmentDateTime = ZonedDateTime.of(localDate, localTime, zone)
        val now = ZonedDateTime.now(zone)

        // hours from now to appointment
        val hoursUntil = Duration.between(now, appointmentDateTime).toHours()

        hoursUntil >= 24       // can cancel only if 24h or more remain
    } catch (e: Exception) {
        // If parsing fails, be safe and disallow cancel
        false
    }
}

private fun isPastAppointment(app: Appointment): Boolean {
    return try {
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

        val localDate = LocalDate.parse(app.date, dateFormatter)
        val localTime = LocalTime.parse(app.timing, timeFormatter)

        val zone = ZoneId.systemDefault()
        val appointmentDateTime = ZonedDateTime.of(localDate, localTime, zone)
        val now = ZonedDateTime.now(zone)

        appointmentDateTime.isBefore(now)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun PastScheduleScreen(navController: NavController, context: Context) {
    val appointments = remember { mutableStateListOf<Appointment>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val firebaseUid = AuthManager.getCurrentUserId() ?: run {
                isLoading = false
                return@LaunchedEffect
            }

            val profile = AuthManager.getCurrentUserProfile()
            val humanId = profile?.humanId ?: run {
                isLoading = false
                return@LaunchedEffect
            }

            val remoteAppointments = UserRepository.getAppointmentsForPatient(humanId)
            val past = remoteAppointments.filter { isPastAppointment(it) }

            appointments.clear()
            appointments.addAll(past)
        } finally {
            isLoading = false
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
                        text = "Past Appointments",
                        color = Color(0xFF4CB7C2),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        bottomBar = { PatientBottomBar(navController) }
    ) { inner ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CB7C2))
                }
            }

            appointments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No past appointments",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(appointments) { app ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    app.date,
                                    color = Color(0xFF4CB7C2),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = app.timing,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = app.doctorFirstName + " " + app.doctorLastName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = app.doctorSpeciality,
                                    color = Color(0xFF4CB7C2)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
