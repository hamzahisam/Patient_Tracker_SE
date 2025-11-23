package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import com.example.patienttracker.data.firebase.UserRepository
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.CircleShape
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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import com.example.patienttracker.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.snapshotFlow
import com.example.patienttracker.data.AppointmentStorage
import com.example.patienttracker.data.Appointment
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import com.example.patienttracker.auth.AuthManager
import androidx.compose.foundation.layout.statusBars
import com.example.patienttracker.ui.screens.doctor.DoctorBottomBar
import com.example.patienttracker.ui.screens.patient.FavoritesScreen


@Composable
fun PatientHomeScreen(navController: NavController, context: Context) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD))
    )

    // Pull name from navigation arguments or saved state
    val firstNameArg = navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: "Patient"
    val lastNameArg = navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: ""

    Scaffold(
        bottomBar = {
            PatientBottomBar(navController)
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(
                gradient = gradient,
                firstName = firstNameArg,
                lastName = lastNameArg,
                navController = navController
            )

            CategoriesRow(
                items = listOf(
                    Category("Favorite", R.drawable.ic_favourites),
                    Category("Doctors", R.drawable.ic_doctors),
                    Category("Specialties", R.drawable.ic_specialties),
                    Category("Record", R.drawable.ic_records),
                ),
                onCategoryClick = { category ->
                    when (category.label) {
                        "Favorite" -> navController.navigate("favorites_screen") // ADD THIS LINE
                        "Doctors" -> navController.navigate("doctor_list/All")
                        "Specialties" -> navController.navigate("patient_specialties")
                        "Record" -> navController.navigate("record_doctor_list")
                    }
                }
            )

            UpcomingSchedule(gradient = gradient, navController = navController)

            SpecialtiesGrid(
                titleGradient = gradient,
                specialties = listOf(
                    Spec("Cardiology", R.drawable.ic_cardiology),
                    Spec("Dermatology", R.drawable.ic_dermatology),
                    Spec("General Medicine", R.drawable.ic_general_medicine),
                    Spec("Gynecology", R.drawable.ic_gynecology),
                    Spec("Odontology", R.drawable.ic_odontology),
                    Spec("Oncology", R.drawable.ic_oncology),
                ),
                onSpecialtyClick = { spec ->
                    navController.navigate("doctor_list/${spec.title}")
                }
            )
        }
    }
}

/* ----------------------------- Header ------------------------------ */

@Composable
private fun HeaderCard(gradient: Brush, firstName: String, lastName: String, navController: NavController) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick actions (placeholders)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(R.drawable.ic_settings) {
                        // Navigate to settings screen
                        navController.navigate("settings")
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Hi,",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        firstName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Avatar placeholder - make it clickable
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF4CB7C2),
                            shape = CircleShape
                        )
                        .clickable {
                            val safeFirstName = firstName.ifBlank { "Patient" }
                            val safeLastName = lastName.ifBlank { "" }
                            navController.navigate("patient_profile/$safeFirstName/$safeLastName")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val initials = buildString {
                        if (firstName.isNotBlank()) append(firstName.first().uppercaseChar())
                        if (lastName.isNotBlank()) append(lastName.first().uppercaseChar())
                    }.ifBlank { "P" }
                    Text(
                        initials,
                        color = Color(0xFF4CB7C2),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun IconBubble(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
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

/* --------------------------- Categories ---------------------------- */

data class Category(val label: String, @DrawableRes val iconRes: Int)

@Composable
private fun CategoriesRow(items: List<Category>, onCategoryClick: (Category) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "Categories",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF4CB7C2),
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { category ->
                CategoryChip(category) { onCategoryClick(it) }
            }
        }
        Divider(
            Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}


@Composable
private fun CategoryChip(
    cat: Category,
    onClick: (Category) -> Unit = {}    // callback for handling click
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color(0xFF4CB7C2))
            ) {
                onClick(cat)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = cat.iconRes),
            contentDescription = cat.label,
            modifier = Modifier
                .size(if (cat.label == "Doctors") 48.dp else if (cat.label == "Favourite") 60.dp else if (cat.label == "Specialties") 66.dp else 58.dp)
                .padding(top = 4.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/* ---------------------- Upcoming Schedule ------------------------- */

private fun generateDateChipsAroundToday(
    pastDays: Int = 15,
    futureDays: Int = 15,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): Pair<List<DayChip>, Int> {
    val today = LocalDate.now(zoneId)
    val start = today.minusDays(pastDays.toLong())
    val total = pastDays + futureDays + 1

    val list = (0 until total).map { offset ->
        val date = start.plusDays(offset.toLong())
        val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        DayChip(date = date, day = date.dayOfMonth.toString(), dow = dow)
    }
    // 'today' will be at index == pastDays
    return list to pastDays
}

private fun monthLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.month.getDisplayName(TextStyle.FULL, locale)

@Composable
private fun UpcomingSchedule(gradient: Brush, navController: NavController) {
    val context = LocalContext.current
    val allAppointments = remember { AppointmentStorage.getAppointments(context) }

    Column(Modifier.fillMaxWidth()) {
        val (dates, todayIndex) = remember { generateDateChipsAroundToday(pastDays = 7, futureDays = 7) }
        val locale = Locale.getDefault()
        var displayedMonth by remember { mutableStateOf(monthLabel(dates[todayIndex].date, locale)) }
        var selected by rememberSaveable { mutableIntStateOf(todayIndex) }

        val listState = rememberLazyListState()

        LaunchedEffect(dates) {
            listState.scrollToItem((todayIndex - 2).coerceAtLeast(0))
        }

        // update month label as scroll changes
        LaunchedEffect(listState, dates) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { idx ->
                    val probeIndex = (idx + 2).coerceIn(0, dates.lastIndex)
                    displayedMonth = monthLabel(dates[probeIndex].date, locale)
                }
        }

        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradient)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Upcoming Schedule",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.weight(1f))
            Text(
                displayedMonth,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge
            )
        }

        // --- Date selector ---
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dates.size) { i ->
                DayPill(
                    dates[i],
                    selected = (i == selected),
                    onClick = {
                        selected = i
                        displayedMonth = monthLabel(dates[i].date, locale)
                    }
                )
            }
        }

        // --- Filter appointments by selected date ---
        val selectedDate = dates[selected].date
        val filtered = remember(selectedDate, allAppointments) {
            allAppointments.filter { appointment ->
                // Parse the appointment date string and compare with selected date
                try {
                    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", locale)
                    val appointmentDate = LocalDate.parse(appointment.date, formatter)
                    appointmentDate == selectedDate
                } catch (e: Exception) {
                    // If parsing fails, try alternative approach
                    appointment.date.contains(selectedDate.dayOfMonth.toString()) &&
                    appointment.date.contains(selectedDate.month.getDisplayName(TextStyle.SHORT, locale))
                }
            }
        }

        // --- Show schedule card ---
        if (filtered.isEmpty()) {
            NoAppointmentsCard(gradient, selectedDate)
        } else {
            ScheduleCard(
                gradient = gradient,
                selectedDate = selectedDate,
                appointments = filtered,
                navController = navController
            )
        }
    }
}


data class DayChip(val date: LocalDate, val day: String, val dow: String)

@Composable
private fun DayPill(item: DayChip, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF4CCAD1) else MaterialTheme.colorScheme.surface
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (!selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(28.dp)
                    )
                } else {
                    Modifier
                }
            )
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = item.day,
            color = fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = item.dow,
            color = fg.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

data class ScheduleEntry(val subtitle: String, val time: String, val doctor: String)

@Composable
private fun ScheduleCard(
    gradient: Brush,
    selectedDate: LocalDate,
    appointments: List<Appointment>,
    navController: NavController
) {
    val locale = Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd", locale)
    val displayDate = selectedDate.format(dateFormatter)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Appointments for $displayDate",
                    color = Color(0xFF2A6C74),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "See all",
                    color = Color(0xFF4CB7C2),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable {
                        navController.navigate("full_schedule")
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            appointments.forEach { appointment ->
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
                        text = appointment.timing,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = primaryTextColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // Doctor + speciality
                    val doctorName = listOf(
                        appointment.doctorFirstName,
                        appointment.doctorLastName
                    ).filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "Unknown doctor" }

                    val speciality = appointment.doctorSpeciality.ifBlank { "Speciality not set" }

                    Text(
                        text = "$doctorName ($speciality)",
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

@Composable
private fun NoAppointmentsCard(gradient: Brush, selectedDate: LocalDate) {
    val locale = Locale.getDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd", locale)
    val displayDate = selectedDate.format(dateFormatter)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
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
}


/* --------------------------- Specialties --------------------------- */

data class Spec(val title: String, @DrawableRes val iconRes: Int)

@Composable
private fun SpecialtiesGrid(
    titleGradient: Brush,
    specialties: List<Spec>,
    onSpecialtyClick: (Spec) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Specialties",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF4CB7C2),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(specialties) { spec ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSpecialtyClick(spec) }
                        .background(MaterialTheme.colorScheme.background)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = spec.iconRes),
                        contentDescription = spec.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}


@Composable
private fun SpecCard(spec: Spec, onClick: (Spec) -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)                      // square, fills the cell
            .clip(RoundedCornerShape(16.dp))
            .semantics { role = Role.Button }     // accessibility
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color(0x3322B7C3))
            ) { onClick(spec) },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = spec.iconRes),
            contentDescription = spec.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),                  // breathing room inside the tile
            contentScale = ContentScale.Fit
        )
    }
}


/* ---------------------- Record: Doctor Selection ------------------- */

data class DoctorRecordEntry(
    val key: String,              // doctorId if available, otherwise doctorName
    val doctorName: String,
    val speciality: String,
    val nextAppointmentLabel: String
)

@Composable
fun PatientRecordDoctorListScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    val locale = remember { Locale.getDefault() }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var patientId by remember { mutableStateOf<String?>(null) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load patient id + appointments from Firestore
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            patientId = profile?.humanId?.takeIf { it.isNotBlank() }

            patientId?.let { id ->
                appointments = UserRepository.getAppointmentsForPatient(id)
            }
        } catch (_: Exception) {
            // keep defaults on error
        } finally {
            isLoading = false
        }
    }

    val today = remember { LocalDate.now() }

    // Build list of doctors for this patient from Firestore appointments
    val doctorEntries = remember(appointments, locale, patientId) {
        // 1) filter to this patient + booked + upcoming
        val relevant = appointments.filter { appt ->
            val matchesPatient = patientId?.let { appt.patientId == it } ?: true
            val isBooked = appt.status.equals("booked", ignoreCase = true)
            val apptDate = parseAppointmentDate(appt.date, locale)
            val isUpcoming = apptDate?.isAfter(today.minusDays(1)) ?: false

            matchesPatient && isBooked && isUpcoming
        }

        // 2) group by doctor
        val grouped = relevant.groupBy { appt ->
            if (appt.doctorId.isNotBlank()) {
                appt.doctorId
            } else {
                listOf(appt.doctorFirstName, appt.doctorLastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "unknown-doctor" }
            }
        }

        // 3) map to DoctorRecordEntry
        grouped.mapNotNull { (key, list) ->
            if (list.isEmpty()) return@mapNotNull null

            val earliestAppt = list.minByOrNull { appt ->
                parseAppointmentDate(appt.date, locale) ?: LocalDate.MAX
            } ?: return@mapNotNull null

            // If we genuinely have no doctor info, skip
            if (
                earliestAppt.doctorId.isBlank() &&
                earliestAppt.doctorFirstName.isBlank() &&
                earliestAppt.doctorLastName.isBlank()
            ) return@mapNotNull null

            val doctorName = listOf(
                earliestAppt.doctorFirstName,
                earliestAppt.doctorLastName
            ).filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Unknown doctor" }

            val displayDoctorName = "Dr. $doctorName".trim()

            val displaySpeciality =
                if (earliestAppt.doctorSpeciality.isNotBlank())
                    earliestAppt.doctorSpeciality
                else
                    "Speciality not set"

            val nextAppointmentLabel = buildString {
                if (earliestAppt.date.isNotBlank()) append(earliestAppt.date)
                if (earliestAppt.timing.isNotBlank()) {
                    if (isNotEmpty()) append("  ")
                    append(earliestAppt.timing)
                }
            }.ifBlank { "Not scheduled" }

            DoctorRecordEntry(
                key = key,
                doctorName = displayDoctorName,
                speciality = displaySpeciality,
                nextAppointmentLabel = nextAppointmentLabel
            )
        }.sortedBy { entry ->
            val datePart = entry.nextAppointmentLabel.substringBefore("  ")
            parseAppointmentDate(datePart, locale) ?: LocalDate.MAX
        }
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = statusBarPadding + 8.dp,
                            bottom = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4CB7C2)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Doctor",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CB7C2)
                    )
                }
            }
        },
        bottomBar = {
            PatientBottomBar(navController)
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CB7C2))
                }
            }

            doctorEntries.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming appointments.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(doctorEntries.size) { index ->
                        val entry = doctorEntries[index]
                        DoctorRecordCard(
                            entry = entry,
                            onClick = {
                                navController.navigate("patient_record_options/${entry.key}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorRecordCard(
    entry: DoctorRecordEntry,
    onClick: () -> Unit
) {
    val accent = Color(0xFF4CB7C2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = accent, shape = RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry.doctorName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = entry.speciality,
                style = MaterialTheme.typography.bodyMedium,
                color = accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Next appointment: ${entry.nextAppointmentLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Helper to parse an appointment date string like "Monday, 24 Nov 2025" into a LocalDate.
 * Returns null if parsing fails.
 */
private fun parseAppointmentDate(raw: String, locale: Locale): LocalDate? {
    // Base string as-is
    val base = raw.trim()

    // Try first by stripping any trailing time / extra info, e.g. "at 10:30 AM", "|", "-", "@"
    val cleaned = base
        .substringBefore(" at")
        .substringBefore(" @")
        .substringBefore("|")
        .substringBefore(" -")
        .trim()

    // 1) Try date-only patterns on the cleaned string
    val dateOnlyPatterns = listOf(
        "EEEE, dd MMM yyyy",
        "EEE, dd MMM yyyy",
        "EEEE, dd MMMM yyyy",
        "EEE, dd MMMM yyyy"
    )

    for (pattern in dateOnlyPatterns) {
        try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            return LocalDate.parse(cleaned, formatter)
        } catch (_: Exception) {
            // ignore and try next
        }
    }

    // 2) Try full string with date+time patterns and then extract the LocalDate
    val dateTimePatterns = listOf(
        "EEEE, dd MMM yyyy 'at' hh:mm a",
        "EEE, dd MMM yyyy 'at' hh:mm a",
        "EEEE, dd MMM yyyy HH:mm",
        "EEE, dd MMM yyyy HH:mm"
    )

    for (pattern in dateTimePatterns) {
        try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            val accessor = formatter.parse(base)
            return LocalDate.from(accessor)
        } catch (_: Exception) {
            // ignore and try next
        }
    }

    // If nothing matched, give up
    return null
}

/* --------------------------- Bottom Bar ---------------------------- */

@Composable
fun PatientBottomBar(
    navController: NavController
) {
    // Load current patient name once
    var firstName by remember { mutableStateOf("Patient") }
    var lastName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            firstName = profile?.firstName ?: "Patient"
            lastName  = profile?.lastName  ?: ""
        } catch (_: Exception) {
            // keep defaults on error
        }
    }

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
                .padding(bottom = bottomInset.coerceAtMost(3.dp))
        ) {
            Divider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                    selected = true
                ) {
                    navController.navigate("patient_home/$firstName/$lastName") {
                        popUpTo("patient_home/$firstName/$lastName") { inclusive = true }
                    }
                }

                BottomItem(
                    iconRes = R.drawable.ic_messages,
                    label = "Chat"
                ) {
                    navController.navigate("chat_selection_patient")
                }

                BottomItem(
                    iconRes = R.drawable.ic_user_profile,
                    label = "Profile"
                ) {
                    val safeFirst = firstName.ifBlank { "Patient" }
                    val safeLast  = lastName.ifBlank { "" }
                    navController.navigate("patient_profile/$safeFirst/$safeLast")
                }

                BottomItem(
                    iconRes = R.drawable.ic_booking,
                    label = "Schedule"
                ) {
                    navController.navigate("full_schedule")
                }
            }
        }
    }
}

@Composable
private fun BottomItem(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {}
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