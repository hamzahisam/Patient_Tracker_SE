package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import com.example.patienttracker.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.patienttracker.auth.AuthManager
import com.example.patienttracker.auth.UserProfile
import androidx.compose.runtime.produceState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import kotlin.text.format

// Helper function to parse the start time in minutes from strings like "6:00 pm – 9:00 pm"
private fun parseAppointmentStartMinutes(time: String): Int {
    val firstPart = time.split("–", "-").firstOrNull()?.trim() ?: return Int.MAX_VALUE
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

// Helper function to parse appointment date strings like "Friday, 28 Nov 2025" into LocalDate
private fun parseAppointmentDateLabel(raw: String, locale: Locale): LocalDate? {
    val cleaned = raw.trim()
        .substringBefore(" at")
        .substringBefore(" @")
        .substringBefore("|")
        .substringBefore(" -")
        .trim()

    val patterns = listOf(
        "EEEE, dd MMM yyyy",
        "EEE, dd MMM yyyy",
        "EEEE, dd MMMM yyyy",
        "EEE, dd MMMM yyyy"
    )

    for (pattern in patterns) {
        try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            return LocalDate.parse(cleaned, formatter)
        } catch (_: DateTimeParseException) {
            // try next pattern
        }
    }
    return null
}

/**
 * Checks if an appointment is in the past based on date and time.
 * For today's appointments, compares the time. For other days, only compares the date.
 */
private fun isAppointmentPastDoctor(apptDate: LocalDate?, timing: String, today: LocalDate, currentMinutes: Int): Boolean {
    if (apptDate == null) return false
    return when {
        apptDate.isBefore(today) -> true
        apptDate.isEqual(today) -> {
            val apptMinutes = parseAppointmentStartMinutes(timing)
            apptMinutes < currentMinutes
        }
        else -> false
    }
}

// ---------- Public entry ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorHomeScreen(
    navController: NavController,
    context: Context,
    firstName: String? = null,
    lastName: String? = null,
    doctorId: String? = null,
    specialty: String? = null
) {
    // Load current user profile once; prefer this over nav args so Home works even when
    // navigated to from the bottom bar.
    val profileState = produceState<UserProfile?>(initialValue = null) {
        value = AuthManager.getCurrentUserProfile()
    }
    val profile = profileState.value

    // Resolve name/ID from profile first, then explicit params, then savedStateHandle / route args, else fallback
    val resolvedFirst = profile?.firstName
        ?: firstName
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: "Doctor"

    val resolvedLast = profile?.lastName
        ?: lastName
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: ""

    val resolvedId = profile?.humanId
        ?: doctorId
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("doctorId")
        ?: navController.currentBackStackEntry?.arguments?.getString("doctorId")
        ?: ""

    val resolvedSpecialty = specialty
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("doctorSpecialty")
        ?: navController.currentBackStackEntry?.arguments?.getString("doctorSpecialty")
        ?: "Specialist"

    val initials = buildString {
        if (resolvedFirst.isNotBlank()) append(resolvedFirst.first().uppercaseChar())
        if (resolvedLast.isNotBlank()) append(resolvedLast.first().uppercaseChar())
    }.ifBlank { "DR" }

    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD))
    )

    Scaffold(
        bottomBar = { DoctorBottomBar(navController, selectedTab = 0) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ){ inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            DoctorHeader(
                gradient = gradient,
                greeting = "Hi,",
                name = "Dr. $resolvedFirst",
                initials = initials,
                onBell = { /* TODO: open notifications */ },
                onSettings = {
                    navController.navigate("doctor_settings")
                },
                onSearch = { /* TODO: open search */ },
                onProfile = {
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("doctorFullName", "Dr. $resolvedFirst $resolvedLast")
                        set("doctorId", resolvedId)
                        set("doctorSpecialty", resolvedSpecialty)
                    }
                    navController.navigate("doctor_profile")
                }
            )

            Spacer(Modifier.height(12.dp))

            // Quick Stats Cards
            QuickStatsSection(
                doctorId = resolvedId,
                navController = navController
            )

            Spacer(Modifier.height(12.dp))

            DoctorScheduleWithUnavailable(
                gradient = gradient,
                doctorId = resolvedId,
                doctorName = "Dr. $resolvedFirst $resolvedLast",
                navController = navController
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------- Header ----------
@Composable
private fun DoctorHeader(
    gradient: Brush,
    greeting: String,
    name: String,
    initials: String,
    onBell: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
) {
    val accent = Color(0xFF4CB7C2)
    
    // Time and date formatting state
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM") }
    var currentTimeText by remember { mutableStateOf(LocalTime.now().format(timeFormatter)) }
    var currentDateText by remember { mutableStateOf(LocalDate.now().format(dateFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeText = LocalTime.now().format(timeFormatter)
            currentDateText = LocalDate.now().format(dateFormatter)
            delay(60_000)
        }
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left: Settings icon
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(iconRes = R.drawable.ic_settings, onClick = onSettings)
                }

                Spacer(Modifier.width(12.dp))

                // Center: current time + date
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTimeText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent
                    )
                    Text(
                        text = currentDateText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = accent.copy(alpha = 0.85f)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Right: greeting + name
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Avatar placeholder - clickable to profile
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            width = 1.dp,
                            color = accent,
                            shape = CircleShape
                        )
                        .clickable { onProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = accent,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IconBubble(@DrawableRes iconRes: Int, onClick: () -> Unit) {
    val accent = Color(0xFF4CB7C2)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = accent, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ---------- Quick Stats Section ----------
@Composable
private fun QuickStatsSection(
    doctorId: String,
    navController: NavController
) {
    val accent = Color(0xFF4CB7C2)
    val db = remember { FirebaseFirestore.getInstance() }
    val locale = Locale.getDefault()
    
    // Stats state
    var todayAppointments by remember { mutableStateOf(0) }
    var totalPatients by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Fetch stats from Firebase
    LaunchedEffect(doctorId) {
        if (doctorId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", locale)
        val todayKey = today.format(formatter)
        
        // Fetch today's remaining booked appointments (exclude past and cancelled)
        db.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("date", todayKey)
            .whereEqualTo("status", "booked")
            .get()
            .addOnSuccessListener { snap ->
                val currentTime = LocalTime.now()
                val currentMinutes = currentTime.hour * 60 + currentTime.minute
                
                // Filter to only count appointments that haven't started yet
                val remainingCount = snap.documents.count { doc ->
                    val timeStr = doc.getString("timing") ?: doc.getString("time") ?: ""
                    val appointmentMinutes = parseAppointmentStartMinutes(timeStr)
                    appointmentMinutes >= currentMinutes
                }
                todayAppointments = remainingCount
                
                // Count unique patients with appointments today
                val uniquePatientsToday = snap.documents
                    .mapNotNull { it.getString("patientId") }
                    .filter { it.isNotBlank() }
                    .distinct()
                totalPatients = uniquePatientsToday.size
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Stats",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        )
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Remaining Appointments Today Card
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Remaining",
                value = if (isLoading) "..." else todayAppointments.toString(),
                subtitle = "Today",
                iconRes = R.drawable.ic_booking,
                onClick = {
                    navController.navigate("doctor_schedule") {
                        launchSingleTop = true
                    }
                }
            )
            
            // Today's Patients Card
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Today",
                value = if (isLoading) "..." else totalPatients.toString(),
                subtitle = "Patients",
                iconRes = R.drawable.ic_record,
                onClick = {
                    navController.navigate("doctor_patients") {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    val accent = Color(0xFF4CB7C2)
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                // Value
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                )
                
                // Subtitle
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// ---------- Mark Not Available Section ----------
@Composable
private fun MarkNotAvailableSection(
    doctorId: String,
    doctorName: String,
    selectedDate: LocalDate,
    doctorDays: List<String>,
    unavailableDates: List<String>,
    isLoadingDoctorData: Boolean,
    onUnavailableDateAdded: (String) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val today = LocalDate.now()
    val locale = Locale.ENGLISH
    // Use dayOfWeek.name for consistent matching (gives "MONDAY", "TUESDAY", etc.)
    val selectedDayName = selectedDate.dayOfWeek.name.lowercase(Locale.ROOT)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", locale)
    val selectedDateFormatted = selectedDate.format(dateFormatter)
    val isSelectedDateInPast = selectedDate.isBefore(today)
    val isSelectedDateToday = selectedDate == today
    
    // Local state for processing
    var isProcessing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var appointmentsToCancel by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var cancelledCount by remember { mutableStateOf(0) }
    
    // Check if selected date is a working day and not already marked unavailable
    // Use take(3) for robust matching (e.g., "mon" matches "monday")
    val isSelectedDayWorkingDay = doctorDays.any { 
        selectedDayName.contains(it.take(3)) || it.contains(selectedDayName.take(3)) 
    }
    val isAlreadyUnavailable = unavailableDates.contains(selectedDateFormatted)
    val canMarkUnavailable = isSelectedDayWorkingDay && !isAlreadyUnavailable && !isLoadingDoctorData && !isSelectedDateInPast
    
    // Dynamic title based on whether it's today or a future date
    val dialogTitle = if (isSelectedDateToday) "Mark Unavailable for Today?" else "Mark Unavailable for $selectedDateFormatted?"
    val buttonText = when {
        isLoadingDoctorData -> "Loading..."
        isSelectedDateInPast -> "Cannot Mark Past Date"
        isAlreadyUnavailable -> "Already Marked Unavailable"
        !isSelectedDayWorkingDay -> "Not a Working Day"
        isProcessing -> "Processing..."
        isSelectedDateToday -> "Mark Not Available for Today"
        else -> "Mark Not Available"
    }
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    dialogTitle,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "This will:",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Mark all time slots as unavailable for this date",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Cancel ${appointmentsToCancel.size} existing appointment(s)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Notify affected patients",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Date: $selectedDateFormatted",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isProcessing = true
                        
                        // Step 1: Add selected date to unavailableDates
                        db.collection("users")
                            .whereEqualTo("humanId", doctorId)
                            .whereEqualTo("role", "doctor")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val docRef = snapshot.documents.firstOrNull()?.reference
                                if (docRef != null) {
                                    docRef.update("unavailableDates", com.google.firebase.firestore.FieldValue.arrayUnion(selectedDateFormatted))
                                        .addOnSuccessListener {
                                            onUnavailableDateAdded(selectedDateFormatted)
                                            
                                            // Step 2: Cancel all appointments for this date
                                            if (appointmentsToCancel.isNotEmpty()) {
                                                val batch = db.batch()
                                                
                                                for (appt in appointmentsToCancel) {
                                                    val apptId = appt["id"] as? String ?: continue
                                                    val patientId = appt["patientId"] as? String ?: ""
                                                    val patientName = appt["patientName"] as? String ?: "Patient"
                                                    val timing = appt["timing"] as? String ?: ""
                                                    
                                                    // Update appointment status to cancelled
                                                    val apptRef = db.collection("appointments").document(apptId)
                                                    batch.update(apptRef, mapOf(
                                                        "status" to "cancelled",
                                                        "cancelledBy" to "doctor",
                                                        "cancellationReason" to "Doctor marked unavailable for the day"
                                                    ))
                                                    
                                                    // Create cancellation notification for patient
                                                    val notificationRef = db.collection("notifications").document()
                                                    batch.set(notificationRef, mapOf(
                                                        "patientId" to patientId,
                                                        "doctorId" to doctorId,
                                                        "doctorName" to doctorName,
                                                        "type" to "appointment_cancelled",
                                                        "title" to "Appointment Cancelled",
                                                        "message" to "Your appointment with $doctorName on $selectedDateFormatted at $timing has been cancelled. The doctor is not available on this date.",
                                                        "date" to selectedDateFormatted,
                                                        "time" to timing,
                                                        "read" to false,
                                                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                    ))
                                                }
                                                
                                                batch.commit()
                                                    .addOnSuccessListener {
                                                        cancelledCount = appointmentsToCancel.size
                                                        isProcessing = false
                                                        showSuccessMessage = true
                                                    }
                                                    .addOnFailureListener {
                                                        isProcessing = false
                                                    }
                                            } else {
                                                isProcessing = false
                                                showSuccessMessage = true
                                            }
                                        }
                                        .addOnFailureListener {
                                            isProcessing = false
                                        }
                                } else {
                                    isProcessing = false
                                }
                            }
                            .addOnFailureListener {
                                isProcessing = false
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
    
    // Success Message Dialog
    if (showSuccessMessage) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Marked Unavailable",
                    color = Color(0xFF4CB7C2),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "You are now marked as unavailable for $selectedDateFormatted.",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (cancelledCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$cancelledCount appointment(s) have been cancelled and patients have been notified.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessMessage = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CB7C2)
                    )
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Button(
            onClick = {
                if (canMarkUnavailable) {
                    // First, fetch appointments to cancel
                    db.collection("appointments")
                        .whereEqualTo("doctorId", doctorId)
                        .whereEqualTo("date", selectedDateFormatted)
                        .whereEqualTo("status", "booked")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            appointmentsToCancel = snapshot.documents.map { doc ->
                                mapOf(
                                    "id" to doc.id,
                                    "patientId" to (doc.getString("patientId") ?: ""),
                                    "patientName" to (doc.getString("patientFirstName") ?: "Patient"),
                                    "timing" to (doc.getString("timing") ?: "")
                                )
                            }
                            showConfirmDialog = true
                        }
                }
            },
            enabled = canMarkUnavailable && !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canMarkUnavailable) Color(0xFFE53935) else Color.Gray.copy(alpha = 0.3f),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = buttonText,
                color = if (canMarkUnavailable) Color.White else Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (!isLoadingDoctorData && isSelectedDayWorkingDay && !isAlreadyUnavailable && !isSelectedDateInPast) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isSelectedDateToday) "This will cancel all today's appointments" else "This will cancel all appointments on $selectedDateFormatted",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE53935).copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ---------- Schedule with Unavailable wrapper ----------
@Composable
private fun DoctorScheduleWithUnavailable(
    gradient: Brush,
    doctorId: String,
    doctorName: String,
    navController: NavController
) {
    val db = remember { FirebaseFirestore.getInstance() }
    
    // Shared selected date state - always start with today
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Reset to today whenever this composable enters composition
    // This handles the case when navigating back from other screens
    LaunchedEffect(Unit) {
        selectedDate = LocalDate.now()
    }
    
    // Load doctor's working days and unavailable dates ONCE at this level
    var doctorDays by remember { mutableStateOf<List<String>>(emptyList()) }
    var unavailableDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingDoctorData by remember { mutableStateOf(true) }
    
    LaunchedEffect(doctorId) {
        if (doctorId.isBlank()) {
            isLoadingDoctorData = false
            return@LaunchedEffect
        }
        
        db.collection("users")
            .whereEqualTo("humanId", doctorId)
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc != null) {
                    val daysList = (doc.get("days") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    doctorDays = daysList.map { it.trim().lowercase() }
                    
                    @Suppress("UNCHECKED_CAST")
                    unavailableDates = (doc.get("unavailableDates") as? List<String>) ?: emptyList()
                }
                isLoadingDoctorData = false
            }
            .addOnFailureListener {
                isLoadingDoctorData = false
            }
    }
    
    // Schedule section (day scroller + appointments list)
    DoctorSchedule(
        gradient = gradient,
        doctorId = doctorId,
        navController = navController,
        selectedDate = selectedDate,
        onSelectedDateChange = { newDate ->
            selectedDate = newDate
        }
    )
    
    Spacer(Modifier.height(16.dp))
    
    // Mark Not Available button (uses selected date)
    MarkNotAvailableSection(
        doctorId = doctorId,
        doctorName = doctorName,
        selectedDate = selectedDate,
        doctorDays = doctorDays,
        unavailableDates = unavailableDates,
        isLoadingDoctorData = isLoadingDoctorData,
        onUnavailableDateAdded = { newDate ->
            unavailableDates = unavailableDates + newDate
        }
    )
}

// ---------- Schedule (day scroller + list) ----------
data class DayChip(val date: LocalDate, val day: String, val dow: String)

private fun monthLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.month.getDisplayName(TextStyle.FULL, locale)

private fun generateDateChipsAroundToday(
    pastDays: Int = 15,
    futureDays: Int = 15,
    locale: Locale = Locale.getDefault()
): Pair<List<DayChip>, Int> {
    val today = LocalDate.now()
    val start = today.minusDays(pastDays.toLong())
    val total = pastDays + futureDays + 1
    val list = (0 until total).map { offset ->
        val d = start.plusDays(offset.toLong())
        DayChip(
            date = d,
            day = d.dayOfMonth.toString(),
            dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        )
    }
    val todayIndex = list.indexOfFirst { it.date == today }.coerceAtLeast(0)
    return list to todayIndex
}

@Composable
private fun DoctorSchedule(
    gradient: Brush,
    doctorId: String,
    navController: NavController,
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit
) {
    val locale = Locale.getDefault()
    val (dates, todayIndex) = remember { generateDateChipsAroundToday(15, 15, locale) }
    // Find the index matching the selectedDate
    val selectedIndex = dates.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 } ?: todayIndex
    var selected by rememberSaveable { mutableIntStateOf(selectedIndex) }
    var displayedMonth by remember { mutableStateOf(monthLabel(dates[todayIndex].date, locale)) }
    var appointmentsForDay by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var apptLoading by remember { mutableStateOf(false) }
    var apptError by remember { mutableStateOf<String?>(null) }
    val db = remember { FirebaseFirestore.getInstance() }
    
    // Sync internal selected state with external selectedDate
    LaunchedEffect(selectedDate) {
        val newIndex = dates.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 } ?: todayIndex
        if (selected != newIndex) {
            selected = newIndex
            displayedMonth = monthLabel(dates[newIndex].date, locale)
        }
    }

    // Header (title + month)
    val accent = Color(0xFF4CB7C2)
    val barColor = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = barColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Upcoming Schedule",
            color = accent,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.weight(1f))
        Text(
            displayedMonth,
            color = accent.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge
        )
    }

    // Day scroller
    val listState = rememberLazyListState()
    LaunchedEffect(dates) {
        val startIndex = (todayIndex - 2).coerceAtLeast(0)
        listState.scrollToItem(startIndex)
    }
    // update month label while scrolling
    LaunchedEffect(listState, dates) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { idx ->
            val probe = (idx + 2).coerceIn(0, dates.lastIndex)
            displayedMonth = monthLabel(dates[probe].date, locale)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val today = LocalDate.now()
        items(dates.size) { i ->
            DayPill(
                chip = dates[i],
                selected = i == selected,
                isToday = dates[i].date == today,
                onClick = {
                    selected = i
                    displayedMonth = monthLabel(dates[i].date, locale)
                    onSelectedDateChange(dates[i].date)
                }
            )
        }
    }

    // Appointments list for selected day
    LaunchedEffect(selected, doctorId) {
        // If we somehow don't have a doctorId yet, clear the list and skip.
        if (doctorId.isBlank()) {
            appointmentsForDay = emptyList()
            apptLoading = false
            apptError = null
            return@LaunchedEffect
        }

        apptLoading = true
        apptError = null

        val selectedDate = dates[selected].date

        // 🔹 Match the format you actually store, e.g. "Monday, 17 Nov 2025"
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", locale)
        val dateKey = selectedDate.format(formatter)

        db.collection("appointments")
            // 🔹 Match your Firestore field names
            .whereEqualTo("doctorId", doctorId)   // field is "doctorId" in your doc
            .whereEqualTo("date", dateKey)        // field is "date": "Monday, 17 Nov 2025"
            .whereEqualTo("status", "booked")     // Only show booked, not cancelled
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { doc ->
                    val time = doc.getString("timing") ?: ""          // field "timing"
                    val first = doc.getString("patientFirstName") ?: ""
                    val last = doc.getString("patientLastName") ?: ""
                    val patientName = listOf(first, last)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "Unknown patient" }

                    val reason = doc.getString("status")
                        ?: doc.getString("reason")
                        ?: doc.getString("visitType")
                        ?: "Appointment"

                    Appointment(
                        time = time,
                        patientName = patientName,
                        reason = reason
                    )
                }.sortedBy { parseAppointmentStartMinutes(it.time) }  // Sort by time ascending

                appointmentsForDay = list
                apptLoading = false
                apptError = null
            }
            .addOnFailureListener { e ->
                appointmentsForDay = emptyList()
                apptLoading = false
                apptError = e.message ?: "Failed to load appointments"
            }
    }

    // Format the selected date for display
    val selectedDate = dates[selected].date
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM dd", locale)
    val displayDate = selectedDate.format(dateFormatter)

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Only show header with date when there are appointments
            if (appointmentsForDay.isNotEmpty() && !apptLoading && apptError == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Appointments for $displayDate",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF2A6C74)
                    )
                    Text(
                        "See all",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF4CB7C2),
                        modifier = Modifier.clickable {
                            navController.navigate("doctor_schedule") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            when {
                apptLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                apptError != null -> {
                    Text(
                        text = apptError ?: "Error loading appointments.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                appointmentsForDay.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No appointments for $displayDate",
                            color = Color(0xFF2A6C74),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Click on other dates to check appointments",
                            color = Color(0xFF6AA8B0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        appointmentsForDay.forEach { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val primaryTextColor = MaterialTheme.colorScheme.onSurface
                                val accentColor = Color(0xFF4CB7C2)

                                // Time
                                Text(
                                    text = item.time,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = primaryTextColor
                                )

                                Spacer(Modifier.width(12.dp))

                                // Patient name with reason
                                Text(
                                    text = "Patient • ${item.patientName}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPill(chip: DayChip, selected: Boolean, isToday: Boolean = false, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) Color(0xFF4FC2C9) else Color.Transparent,
        label = "pill-bg"
    )
    val borderColor = when {
        selected -> Color(0xFF4FC2C9)
        isToday -> Color(0xFF2E9E6E) // Green border for today
        else -> Color(0xFF4CB7C2)
    }
    val borderWidth = if (isToday && !selected) 2.dp else 1.dp
    val fg = if (selected) Color.White else Color(0xFF4CB7C2)

    Column(
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(40.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(chip.day, color = fg, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(2.dp))
        Text(chip.dow, color = fg.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
    }
}

// ---------- Appointments ----------
data class Appointment(
    val time: String,
    val patientName: String,
    val reason: String
)

private fun sampleAppointmentsFor(date: LocalDate): List<Appointment> {
    // Simple demo data; plug into your real repo later
    return when (date.dayOfWeek.value % 3) {
        0 -> listOf(
            Appointment("09:00 am", "John Carter", "Follow-up"),
            Appointment("10:30 am", "Maria Lopez", "Lab results"),
            Appointment("01:00 pm", "Wei Zhang", "Initial consult")
        )
        1 -> listOf(
            Appointment("11:00 am", "Amir Khan", "Skin rash"),
            Appointment("02:15 pm", "Emily Brown", "Prescription renewal")
        )
        else -> emptyList()
    }
}

// ---------- Bottom bar ----------
@Composable
fun DoctorBottomBar(
    navController: NavController,
    selectedTab: Int   // 0 = Home, 1 = Chat, 2 = Patients, 3 = Schedule
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp + bottomInset)
                .padding(bottom = bottomInset.coerceAtMost(6.dp))
        ) {
            Divider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = Color.White.copy(alpha = 0.1f)
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomItem(
                    iconRes = R.drawable.ic_home,
                    label = "Home",
                    selected = selectedTab == 0,
                    onClick = {
                        if (selectedTab != 0) {
                            // Find and pop back to doctor_home
                            val popped = navController.popBackStack("doctor_home/{firstName}/{lastName}/{doctorId}", inclusive = false)
                            if (!popped) {
                                // Fallback: just pop until we can't anymore or navigate fresh
                                navController.navigate("doctor_home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                )
                BottomItem(
                    iconRes = R.drawable.ic_messages,
                    label = "Chat",
                    selected = selectedTab == 1,
                    onClick = {
                        if (selectedTab != 1) {
                            navController.navigate("doctor_chat_inbox") {
                                launchSingleTop = true
                            }
                        }
                    }
                )
                BottomItem(
                    iconRes = R.drawable.ic_record,
                    label = "Records",
                    selected = selectedTab == 2,
                    onClick = {
                        if (selectedTab != 2) {
                            navController.navigate("doctor_patients") {
                                launchSingleTop = true
                            }
                        }
                    }
                )
                BottomItem(
                    iconRes = R.drawable.ic_booking,
                    label = "Schedule",
                    selected = selectedTab == 3,
                    onClick = {
                        if (selectedTab != 3) {
                            navController.navigate("doctor_schedule") {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}
// ---------- Patients List ----------
data class DoctorPatientItem(
    val patientId: String,
    val firstName: String,
    val lastName: String,
    val appointmentDate: String,
    val isUpcoming: Boolean    // true if next appointment, false if last appointment
)

@Composable
private fun BottomItem(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun DoctorProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val prevEntry = navController.previousBackStackEntry
    val fullName = prevEntry?.savedStateHandle?.get<String>("doctorFullName") ?: "Doctor"
    val doctorId = prevEntry?.savedStateHandle?.get<String>("doctorId") ?: ""
    val specialty = prevEntry?.savedStateHandle?.get<String>("doctorSpecialty") ?: "Specialist"

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F8FC))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { navController.popBackStack() },
                    tint = Color(0xFF1C3D5A)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C3D5A)
                )
            }
        },
        bottomBar = { DoctorBottomBar(navController, selectedTab = 2) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0D3B40)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF1C3D5A)
                )
                Text(
                    text = "Doctor ID: $doctorId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF35536B)
                )
                Text(
                    text = "Specialty: $specialty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF35536B)
                )
            }

            Button(
                onClick = {
                    Firebase.auth.signOut()
                    navController.navigate("role") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Log out",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
@Composable
fun DoctorPatientsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val locale = remember { Locale.getDefault() }

    var patients by remember { mutableStateOf<List<DoctorPatientItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            val doctorId = profile?.humanId

            if (doctorId.isNullOrBlank()) {
                error = "Could not determine doctor ID."
                loading = false
                return@LaunchedEffect
            }

            val today = LocalDate.now()
            val tenDaysAgo = today.minusDays(10)
            val currentMinutes = LocalTime.now().let { it.hour * 60 + it.minute }

            db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", "booked")
                .get()
                .addOnSuccessListener { snap ->
                    val grouped = snap.documents
                        .groupBy { it.getString("patientId") ?: "" }
                        .filterKeys { it.isNotBlank() }

                    // Filter to only include patients whose most recent appointment is within 10 days
                    val filteredList = grouped.mapNotNull { (pid, docs) ->
                        // Find the most recent appointment date for this patient
                        val mostRecentDate = docs.mapNotNull { doc ->
                            val dateStr = doc.getString("date") ?: ""
                            parseAppointmentDateLabel(dateStr, locale)
                        }.maxOrNull()

                        // Only include if most recent appointment is within 10 days
                        if (mostRecentDate == null || mostRecentDate.isBefore(tenDaysAgo)) {
                            return@mapNotNull null
                        }

                        // Separate upcoming and past appointments (time-sensitive for today)
                        val upcomingDocs = docs.filter { doc ->
                            val dateStr = doc.getString("date") ?: ""
                            val timing = doc.getString("timing") ?: doc.getString("time") ?: ""
                            val apptDate = parseAppointmentDateLabel(dateStr, locale)
                            !isAppointmentPastDoctor(apptDate, timing, today, currentMinutes)
                        }
                        val pastDocs = docs.filter { doc ->
                            val dateStr = doc.getString("date") ?: ""
                            val timing = doc.getString("timing") ?: doc.getString("time") ?: ""
                            val apptDate = parseAppointmentDateLabel(dateStr, locale)
                            isAppointmentPastDoctor(apptDate, timing, today, currentMinutes)
                        }

                        // Determine which appointment to show
                        val (displayDoc, isUpcoming) = if (upcomingDocs.isNotEmpty()) {
                            // Show earliest upcoming appointment (by date then time)
                            val earliest = upcomingDocs.minWithOrNull(
                                compareBy(
                                    { parseAppointmentDateLabel(it.getString("date") ?: "", locale) ?: LocalDate.MAX },
                                    { parseAppointmentStartMinutes(it.getString("timing") ?: it.getString("time") ?: "") }
                                )
                            )!!
                            earliest to true
                        } else if (pastDocs.isNotEmpty()) {
                            // Show most recent past appointment (by date then time)
                            val mostRecent = pastDocs.maxWithOrNull(
                                compareBy(
                                    { parseAppointmentDateLabel(it.getString("date") ?: "", locale) ?: LocalDate.MIN },
                                    { parseAppointmentStartMinutes(it.getString("timing") ?: it.getString("time") ?: "") }
                                )
                            )!!
                            mostRecent to false
                        } else {
                            docs.first() to false
                        }

                        val first = displayDoc.getString("patientFirstName") ?: ""
                        val last = displayDoc.getString("patientLastName") ?: ""
                        val date = displayDoc.getString("date") ?: ""
                        val timing = displayDoc.getString("timing") ?: displayDoc.getString("time") ?: ""
                        
                        val appointmentLabel = buildString {
                            if (date.isNotBlank()) append(date)
                            if (timing.isNotBlank()) {
                                if (isNotEmpty()) append("  ")
                                append(timing)
                            }
                        }
                        
                        DoctorPatientItem(
                            patientId = pid,
                            firstName = first,
                            lastName = last,
                            appointmentDate = appointmentLabel,
                            isUpcoming = isUpcoming
                        )
                    }.sortedBy { it.firstName.lowercase() }

                    patients = filteredList
                    loading = false
                    error = null
                }
                .addOnFailureListener { e ->
                    patients = emptyList()
                    loading = false
                    error = e.message ?: "Failed to load patients"
                }
        } catch (e: Exception) {
            patients = emptyList()
            loading = false
            error = e.message
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
                        .size(28.dp)
                        .clickable { navController.popBackStack() },
                    tint = Color(0xFF4CB7C2)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "My Patients",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4CB7C2)
                )
            }
        },
        bottomBar = { DoctorBottomBar(navController, selectedTab = 2) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
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
                patients.isEmpty() -> {
                    Text(
                        text = "No patients have booked with you yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(patients) { patient ->
                            DoctorPatientRow(patient) {
                                val fullName = listOf(patient.firstName, patient.lastName)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")

                                // Pass selected patient info via SavedStateHandle
                                navController.currentBackStackEntry?.savedStateHandle?.apply {
                                        set("selectedPatientId", patient.patientId)
                                        set("selectedPatientName", fullName)
                                    }
                                navController.navigate("doctor_patient_record_options")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorPatientRow(item: DoctorPatientItem, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF4CB7C2), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = listOf(item.firstName, item.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Patient ${item.patientId}" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ID: ${item.patientId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.appointmentDate.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (item.isUpcoming) "Next appointment: ${item.appointmentDate}" else "Last appointment: ${item.appointmentDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}