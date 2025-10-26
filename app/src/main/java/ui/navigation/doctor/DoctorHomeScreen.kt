package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import com.example.patienttracker.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ---------- Public entry ----------
@Composable
fun DoctorHomeScreen(
    navController: NavController,
    context: Context,
    firstName: String? = null,
    lastName: String? = null,
    doctorId: String? = null
) {
    // Resolve name/ID from explicit params, then savedStateHandle, then route args, else fallback
    val resolvedFirst = firstName
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: "Doctor"

    val resolvedLast = lastName
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: ""

    val resolvedId = doctorId
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("doctorId")
        ?: navController.currentBackStackEntry?.arguments?.getString("doctorId")
        ?: ""

    val initials = buildString {
        if (resolvedFirst.isNotBlank()) append(resolvedFirst.first().uppercaseChar())
        if (resolvedLast.isNotBlank()) append(resolvedLast.first().uppercaseChar())
    }.ifBlank { "DR" }

    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD))
    )

    Scaffold(
        bottomBar = { DoctorBottomBar() },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            DoctorHeader(
                gradient = gradient,
                greeting = "Hi, welcome back",
                name = "Dr. $resolvedFirst $resolvedLast",
                initials = initials,
                onBell = { /* TODO: open notifications */ },
                onSettings = { /* TODO: open settings */ },
                onSearch = { /* TODO: open search */ }
            )

            Spacer(Modifier.height(12.dp))

            DoctorSchedule(gradient = gradient)
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
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color(0xFFF7F9FC),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: circular icon bubbles
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(iconRes = R.drawable.ic_notifications, onClick = onBell)
                IconBubble(iconRes = R.drawable.ic_settings, onClick = onSettings)
                IconBubble(iconRes = R.drawable.ic_search, onClick = onSearch)
            }

            Spacer(Modifier.weight(1f))

            // RIGHT: greeting + name + avatar
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    greeting,
                    color = Color(0xFF5AA8AC),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    name,
                    color = Color(0xFF1C3D5A),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDBE6EF)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color(0xFF1C3D5A), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun IconBubble(@DrawableRes iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFE9F3F9))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
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
private fun DoctorSchedule(gradient: Brush) {
    val locale = Locale.getDefault()
    val (dates, todayIndex) = remember { generateDateChipsAroundToday(15, 15, locale) }
    var selected by rememberSaveable { mutableIntStateOf(todayIndex) }
    var displayedMonth by remember { mutableStateOf(monthLabel(dates[todayIndex].date, locale)) }

    // Header (title + month)
    Surface(color = Color.Transparent) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Upcoming Schedule",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                displayedMonth,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge
            )
        }
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
        items(dates.size) { i ->
            DayPill(
                chip = dates[i],
                selected = i == selected,
                onClick = {
                    selected = i
                    displayedMonth = monthLabel(dates[i].date, locale)
                }
            )
        }
    }

    // Appointments list for selected day
    val appts = remember(selected) { sampleAppointmentsFor(dates[selected].date) }

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFF7F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Appointments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2B6F75)
                )
                Text(
                    "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF2B6F75)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (appts.isEmpty()) {
                Text(
                    "No appointments for this day.",
                    color = Color(0xFF5F6970),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(appts) { item ->
                        AppointmentCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPill(chip: DayChip, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) Color(0xFF4FC2C9) else Color(0xFFEFF7F9),
        label = "pill-bg"
    )
    val fg = if (selected) Color.White else Color(0xFF295B62)

    Column(
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(bg)
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

@Composable
private fun AppointmentCard(item: Appointment) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    item.time,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0D3B40)
                )
                Text(
                    item.reason,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF2B6F75)
                )
            }
            Spacer(Modifier.height(6.dp))
            Divider(color = Color(0x1A000000))
            Spacer(Modifier.height(6.dp))
            Text(
                "Patient • ${item.patientName}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0D3B40)
            )
        }
    }
}

// ---------- Bottom bar ----------
@Composable
private fun DoctorBottomBar() {
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
                .padding(bottom = bottomInset.coerceAtMost(6.dp))
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
                BottomItem(R.drawable.ic_home, "Home", selected = true)
                BottomItem(R.drawable.ic_messages, "Chat")
                BottomItem(R.drawable.ic_user_profile, "Patients") // reuse user icon
                BottomItem(R.drawable.ic_booking, "Schedule")
            }
        }
    }
}

@Composable
private fun BottomItem(@DrawableRes iconRes: Int, label: String, selected: Boolean = false) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { /* TODO: hook up navigation */ },
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