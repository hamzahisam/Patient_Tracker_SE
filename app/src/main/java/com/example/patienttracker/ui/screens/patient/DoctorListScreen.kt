package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.patienttracker.ui.screens.common.BackButton
import kotlinx.parcelize.Parcelize
import com.example.patienttracker.data.FavoritesManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.patienttracker.R

@Parcelize
data class DoctorFull(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val speciality: String,
    val days: String,      // human-readable, e.g. "Mon, Wed, Fri"
    val timings: String,   // human-readable, e.g. "6:00 pm – 9:00 pm"
    val fees: String,      // e.g. "Rs. 500" or "500"
    val clinicAddress: String  // e.g. "123 Medical Plaza, Karachi"
) : Parcelable

@OptIn(ExperimentalMaterial3Api::class)
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
    var searchQuery by remember { mutableStateOf("") }
    
    // LazyColumn state for auto-scroll
    val listState = rememberLazyListState()
    
    // Day and Time filter state
    var selectedDay by remember { mutableStateOf<String?>(null) } // Day name like "Mon", "Tue", etc.
    var selectedTime by remember { mutableStateOf<Int?>(null) } // Time in HHmm format (e.g., 1400 for 2:00 PM)
    var showDayPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Auto-scroll to top when search query or filters change
    LaunchedEffect(searchQuery, selectedDay, selectedTime) {
        listState.animateScrollToItem(0)
    }
    
    // Get current user ID and load favorites
    var favoriteDoctorIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) {
        val userId = com.example.patienttracker.auth.AuthManager.getCurrentUserProfile()?.humanId
        if (userId != null) {
            favoriteDoctorIds = FavoritesManager.getFavorites(context, userId)
        }
    }

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
                    
                    // Get fees - handle both string and number formats
                    val fees = when (val feesRaw = doc.get("fees")) {
                        is String -> feesRaw
                        is Long -> "Rs. $feesRaw"
                        is Double -> "Rs. ${feesRaw.toInt()}"
                        else -> ""
                    }
                    
                    val clinicAddress = doc.getString("clinicAddress") ?: ""

                    DoctorFull(
                        id = id,
                        firstName = first,
                        lastName = last,
                        email = email,
                        phone = phone,
                        speciality = speciality,
                        days = daysDisplay,
                        timings = timingsDisplay,
                        fees = fees,
                        clinicAddress = clinicAddress
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

    // Apply specialty, search, day, and time filters
    val filtered = remember(doctors, specialityFilter, searchQuery, selectedDay, selectedTime) {
        derivedStateOf {
            var result = doctors

            // First apply specialty filter
            if (!specialityFilter.isNullOrBlank() && specialityFilter != "All") {
                result = result.filter { it.speciality.contains(specialityFilter, ignoreCase = true) }
            }

            // Then apply search filter (searches name, clinic address, and speciality)
            if (searchQuery.isNotBlank()) {
                result = result.filter { doctor ->
                    val fullName = "Dr. ${doctor.firstName} ${doctor.lastName}"
                    fullName.contains(searchQuery, ignoreCase = true) ||
                            doctor.firstName.contains(searchQuery, ignoreCase = true) ||
                            doctor.lastName.contains(searchQuery, ignoreCase = true) ||
                            doctor.clinicAddress.contains(searchQuery, ignoreCase = true) ||
                            doctor.speciality.contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Apply day filter
            if (selectedDay != null) {
                result = result.filter { doctor ->
                    doctor.days.contains(selectedDay!!, ignoreCase = true)
                }
            }
            
            // Apply time filter
            if (selectedTime != null) {
                result = result.filter { doctor ->
                    // Parse the timings string to check if selected time is within range
                    isDoctorAvailableAtTime(doctor.timings, selectedTime!!)
                }
            }

            result
        }
    }.value

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 8.dp, top = 28.dp, bottom = 12.dp), // lowers position
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(
                        navController = navController,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = specialityFilter?.ifBlank { "All Doctors" } ?: "All Doctors",
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
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        keyboardController?.hide()
                    })
                }
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar with Date and Time filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Date filter button
                FilterIconButton(
                    icon = Icons.Default.CalendarToday,
                    isActive = selectedDay != null,
                    contentDescription = "Filter by day",
                    onClick = { showDayPicker = true }
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Time filter button
                FilterIconButton(
                    icon = Icons.Default.Schedule,
                    isActive = selectedTime != null,
                    contentDescription = "Filter by time",
                    onClick = { showTimePicker = true }
                )
            }
            
            // Show active filters
            if (selectedDay != null || selectedTime != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedDay != null) {
                        val fullDayName = when (selectedDay) {
                            "Mon" -> "Monday"
                            "Tue" -> "Tuesday"
                            "Wed" -> "Wednesday"
                            "Thu" -> "Thursday"
                            "Fri" -> "Friday"
                            "Sat" -> "Saturday"
                            "Sun" -> "Sunday"
                            else -> selectedDay!!
                        }
                        FilterChip(
                            selected = true,
                            onClick = { selectedDay = null },
                            label = { Text(fullDayName) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear day filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CB7C2).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF4CB7C2)
                            )
                        )
                    }
                    if (selectedTime != null) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedTime = null },
                            label = { Text(formatTimeHHmm(selectedTime!!)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear time filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CB7C2).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF4CB7C2)
                            )
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background), // FIX 2: Changed from hardcoded to theme
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (searchQuery.isNotBlank()) {
                                    "No doctors found for \"$searchQuery\""
                                } else {
                                    "No doctors available"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF6AA8B0),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    "Try searching with different keywords",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8DC2C8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { doc ->
                        DoctorCard(
                            doctor = doc,
                            isFavorited = doc.id in favoriteDoctorIds,
                            onFavoriteToggle = {
                                if (doc.id in favoriteDoctorIds) {
                                    FavoritesManager.removeFavorite(context, doc.id)
                                    favoriteDoctorIds = favoriteDoctorIds - doc.id
                                } else {
                                    FavoritesManager.addFavorite(context, doc.id)
                                    favoriteDoctorIds = favoriteDoctorIds + doc.id
                                }
                            },
                            onBookClick = {
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("selectedDoctor", doc)

                                navController.navigate("book_appointment")
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Day Picker Dialog
    if (showDayPicker) {
        val days = listOf(
            "Mon" to "Monday",
            "Tue" to "Tuesday",
            "Wed" to "Wednesday",
            "Thu" to "Thursday",
            "Fri" to "Friday",
            "Sat" to "Saturday",
            "Sun" to "Sunday"
        )
        
        AlertDialog(
            onDismissRequest = { showDayPicker = false },
            title = { Text("Select Day") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { (shortName, fullName) ->
                        Surface(
                            onClick = {
                                selectedDay = shortName
                                showDayPicker = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDay == shortName) 
                                Color(0xFF4CB7C2).copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = fullName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = if (selectedDay == shortName) 
                                    Color(0xFF4CB7C2) 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDayPicker = false }) {
                    Text("Cancel", color = Color(0xFF4CB7C2))
                }
            }
        )
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val currentHour = selectedTime?.let { it / 100 } ?: 12
        val currentMinute = selectedTime?.let { it % 100 } ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = false
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = timePickerState.hour * 100 + timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK", color = Color(0xFF4CB7C2))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = Color(0xFF4CB7C2))
                }
            },
            title = { Text("Select Time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = Color(0xFF4CB7C2),
                            timeSelectorSelectedContainerColor = Color(0xFF4CB7C2),
                            timeSelectorSelectedContentColor = Color.White
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                "Search name, speciality, location...",
                color = Color(0xFF8DC2C8)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF4CB7C2)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSearchQueryChanged("")
                        keyboardController?.hide()
                    }
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Color(0xFF4CB7C2)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
            }
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color(0xFF4CB7C2),
            unfocusedIndicatorColor = Color(0xFFB9E3E7),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FilterIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFF4CB7C2).copy(alpha = 0.2f) else MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActive) Color(0xFF4CB7C2) else Color(0xFFB9E3E7)
        ),
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) Color(0xFF4CB7C2) else Color(0xFF8DC2C8),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Check if a doctor is available at a specific time based on their timings string
 * Timings format: "6:00 pm – 9:00 pm"
 */
private fun isDoctorAvailableAtTime(timings: String, selectedTime: Int): Boolean {
    if (timings.isBlank()) return false
    
    try {
        // Parse the timings string (e.g., "6:00 pm – 9:00 pm")
        val parts = timings.split("–", "-").map { it.trim() }
        if (parts.size != 2) return false
        
        val startTime = parseTimeToHHmm(parts[0])
        val endTime = parseTimeToHHmm(parts[1])
        
        if (startTime == null || endTime == null) return false
        
        return selectedTime in startTime..endTime
    } catch (e: Exception) {
        return false
    }
}

/**
 * Parse time string like "6:00 pm" to HHmm format (1800)
 */
private fun parseTimeToHHmm(timeStr: String): Int? {
    try {
        val cleaned = timeStr.trim().lowercase()
        val isPm = cleaned.contains("pm")
        val isAm = cleaned.contains("am")
        
        val timePart = cleaned.replace("am", "").replace("pm", "").trim()
        val colonParts = timePart.split(":")
        
        if (colonParts.isEmpty()) return null
        
        var hours = colonParts[0].trim().toIntOrNull() ?: return null
        val minutes = if (colonParts.size > 1) colonParts[1].trim().toIntOrNull() ?: 0 else 0
        
        // Convert to 24-hour format
        if (isPm && hours != 12) {
            hours += 12
        } else if (isAm && hours == 12) {
            hours = 0
        }
        
        return hours * 100 + minutes
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun DoctorCard(
    doctor: DoctorFull,
    isFavorited: Boolean,
    onFavoriteToggle: () -> Unit,
    onBookClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Add favorite icon row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = doctor.speciality,
                        color = Color(0xFF4CB7C2),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    if (doctor.days.isNotBlank()) {
                        Text(
                            "Days: ${doctor.days}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    if (doctor.timings.isNotBlank()) {
                        Text(
                            "Timings: ${doctor.timings}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    if (doctor.fees.isNotBlank()) {
                        Text(
                            "Fees: ${doctor.fees}",
                            color = Color(0xFF4CB7C2),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    if (doctor.clinicAddress.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color(0xFF8DC2C8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = doctor.clinicAddress,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Heart icon for favorites
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorited) R.drawable.ic_favourite_filled 
                                 else R.drawable.ic_favourite_outline
                        ),
                        contentDescription = if (isFavorited) "Remove from favorites" 
                                           else "Add to favorites",
                        tint = if (isFavorited) Color(0xFFFF6B6B) else Color(0xFF8DC2C8)
                    )
                }
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