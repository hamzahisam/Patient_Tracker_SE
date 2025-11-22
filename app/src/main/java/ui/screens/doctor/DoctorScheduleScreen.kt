package com.example.patienttracker.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private const val ACCENT_HEX = 0xFF4CB7C2

// Formatter for labels like "Monday, 24 Nov 2025"
private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault())

private fun parseDateLabel(label: String): LocalDate? = try {
    LocalDate.parse(label, dayFormatter)
} catch (e: DateTimeParseException) {
    null
}

// Parse the *start* time in minutes from strings like "6:00 pm – 9:00 pm"
private fun parseStartMinutes(time: String): Int {
    val firstPart = time.split("–").firstOrNull()?.trim() ?: return Int.MAX_VALUE
    val pieces = firstPart.split(" ")
    if (pieces.isEmpty()) return Int.MAX_VALUE

    val timePart = pieces[0]
    val amPm = pieces.getOrNull(1)?.lowercase(Locale.getDefault()) ?: "am"

    val hm = timePart.split(":")
    val hour12 = hm.getOrNull(0)?.toIntOrNull() ?: return Int.MAX_VALUE
    val minute = hm.getOrNull(1)?.toIntOrNull() ?: 0

    var hour24 = hour12 % 12
    if (amPm == "pm") {
        hour24 += 12
    }
    return hour24 * 60 + minute
}

data class DoctorScheduleItem(
    val dateLabel: String,
    val time: String,
    val patientName: String,
    val status: String
)

@Composable
fun DoctorScheduleScreen(navController: NavController) {
    val db = remember { FirebaseFirestore.getInstance() }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<DoctorScheduleItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            val doctorId = profile?.humanId

            if (doctorId.isNullOrBlank()) {
                error = "Could not determine doctor ID."
                loading = false
                return@LaunchedEffect
            }

            // Load all appointments for this doctor
            val snapshot = db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                val dateLabel = doc.getString("date") ?: return@mapNotNull null

                val time = doc.getString("timing")
                    ?: doc.getString("time")
                    ?: ""

                val first = doc.getString("patientFirstName") ?: ""
                val last = doc.getString("patientLastName") ?: ""
                val patientName = listOf(first, last)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Unknown patient" }

                val status = doc.getString("status") ?: "Appointment"

                DoctorScheduleItem(
                    dateLabel = dateLabel,
                    time = time,
                    patientName = patientName,
                    status = status
                )
            }
                .sortedWith(
                    compareBy<DoctorScheduleItem>(
                        { parseDateLabel(it.dateLabel) ?: LocalDate.MAX },
                        { parseStartMinutes(it.time) }
                    )
                )

            items = list
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Failed to load schedule."
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { navController.popBackStack() },
                    tint = Color(ACCENT_HEX)
                )
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(ACCENT_HEX)
                )
            }
        },
        bottomBar = { DoctorBottomBar(navController, selectedTab = 2) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                items.isEmpty() -> {
                    Text(
                        text = "No upcoming appointments.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    // Group by date label
                    val grouped = items.groupBy { it.dateLabel }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                    ) {
                        grouped.forEach { (dateLabel, dayItems) ->
                            item(key = "header_$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(ACCENT_HEX),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(dayItems) { appt ->
                                ScheduleCard(appt)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(item: DoctorScheduleItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.time.ifBlank { "Time not set" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.status,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(ACCENT_HEX)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Patient • ${item.patientName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}