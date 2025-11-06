package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
            BottomBar(
                navController = navController,
                firstName = firstNameArg,
                lastName = lastNameArg
            ) 
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        "Doctors" -> navController.navigate("doctor_list/All")
                        "Specialties" -> navController.navigate("doctor_list/All") // optional
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
        color = Color(0xFFF6F8FC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick actions (placeholders)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(R.drawable.ic_notifications) { /* TODO: handle notification click */ }
                    IconBubble(R.drawable.ic_settings) { /* TODO: handle notification click */ }
                    IconBubble(R.drawable.ic_search) { /* TODO: handle notification click */ }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Hi,",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF6AA8B0),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        firstName, // Changed from "$firstName $lastName" to just firstName
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C3D5A)
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Avatar placeholder - make it clickable
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCAD9E6))
                        .clickable { 
                            // Safe navigation with fallback
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
                    Text(initials)
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
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFE9F3F6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
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
        Divider(Modifier.padding(top = 12.dp), color = Color(0xFFE5EFF3))
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
                .size(52.dp) // consistent icon size
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
    val bg = if (selected) Color(0xFF4CCAD1) else Color(0xFFEAF7F8)
    val fg = if (selected) Color.White else Color(0xFF2C6C73)
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable { onClick() } // Add this line to make it clickable
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
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFEAF7F8), Color(0xFFD5F1F4))
                    )
                )
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.clickable { navController.navigate("full_schedule") }
                )
            }
            
            Spacer(Modifier.height(12.dp))

            appointments.forEachIndexed { index, appointment ->
                Column {
                    Text(
                        appointment.time,
                        color = Color(0xFF2A6C74), 
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            appointment.time,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2A6C74)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${appointment.doctorName} (${appointment.speciality})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2A6C74)
                        )
                    }
                    if (index != appointments.lastIndex) {
                        Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFB9E3E7))
                    }
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
                .background(
                    Brush.linearGradient(listOf(Color(0xFFEAF7F8), Color(0xFFD5F1F4)))
                )
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(specialties) { spec ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSpecialtyClick(spec) }
                        .background(Color(0xFFEAF7F8))
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = spec.iconRes),
                        contentDescription = spec.title,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        spec.title,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2A6C74),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
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

/* --------------------------- Bottom Bar ---------------------------- */

@Composable
private fun BottomBar(
    navController: NavController,
    firstName: String,
    lastName: String
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color(0xFFF6F8FC)
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
                color = Color(0x14000000)
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
                ) { /* navController.navigate("chat_screen") */ }

                BottomItem(
                    iconRes = R.drawable.ic_user_profile,
                    label = "Profile"
                ) { 
                    val safeFirstName = firstName.ifBlank { "Patient" }
                    val safeLastName = lastName.ifBlank { "" }
                    navController.navigate("patient_profile/$safeFirstName/$safeLastName") 
                }

                BottomItem(
                    iconRes = R.drawable.ic_booking,
                    label = "Schedule"
                ) { navController.navigate("full_schedule") }
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
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF5F6970)
        )
    }
}