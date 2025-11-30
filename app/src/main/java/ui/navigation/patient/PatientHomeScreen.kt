package com.example.patienttracker.ui.screens.patient

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import com.example.patienttracker.data.AppointmentStorage
import com.example.patienttracker.data.Appointment
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import com.example.patienttracker.auth.AuthManager
import androidx.compose.foundation.layout.statusBars
import com.example.patienttracker.ui.screens.doctor.DoctorBottomBar
import com.example.patienttracker.ui.screens.patient.FavoritesScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

// Data class for cancellation notification
data class CancellationNotification(
    val id: String,
    val doctorName: String,
    val date: String,
    val time: String,
    val message: String
)

// Helper composable for detail rows in notification popup
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            color = Color(0xFFE53935),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PatientHomeScreen(navController: NavController, context: Context) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF8DEBEE), Color(0xFF3CC7CD))
    )

    // Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    // Handle back button press
    BackHandler {
        showExitDialog = true
    }

    // Pull name from navigation arguments or saved state
    val firstNameArg = navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: "Patient"
    val lastNameArg = navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: ""
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Exit App",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Are you sure you want to quit?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        if (activity != null) {
                            activity.finishAffinity()
                        } else {
                            kotlin.system.exitProcess(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Yes, Exit", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CB7C2)
                    )
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Cancellation notification state
    var showCancellationPopup by remember { mutableStateOf(false) }
    var cancellationNotification by remember { mutableStateOf<CancellationNotification?>(null) }
    val db = remember { FirebaseFirestore.getInstance() }
    
    // Check for unread cancellation notifications
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            val patientId = profile?.humanId ?: return@LaunchedEffect
            
            db.collection("notifications")
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("type", "appointment_cancelled")
                .whereEqualTo("read", false)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    if (doc != null) {
                        cancellationNotification = CancellationNotification(
                            id = doc.id,
                            doctorName = doc.getString("doctorName") ?: "Doctor",
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: "",
                            message = doc.getString("message") ?: "Your appointment has been cancelled."
                        )
                        showCancellationPopup = true
                    }
                }
        } catch (e: Exception) {
            // Ignore errors
        }
    }
    
    // Cancellation notification popup
    if (showCancellationPopup && cancellationNotification != null) {
        AlertDialog(
            onDismissRequest = {
                // Mark as read and dismiss
                db.collection("notifications")
                    .document(cancellationNotification!!.id)
                    .update("read", true)
                showCancellationPopup = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "⚠️ Appointment Cancelled",
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        cancellationNotification!!.message,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Details card
                    Surface(
                        color = Color(0xFFE53935).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            DetailRow("Doctor", cancellationNotification!!.doctorName)
                            Spacer(Modifier.height(4.dp))
                            DetailRow("Date", cancellationNotification!!.date)
                            Spacer(Modifier.height(4.dp))
                            DetailRow("Time", cancellationNotification!!.time)
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Please book a new appointment at your convenience.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("notifications")
                            .document(cancellationNotification!!.id)
                            .update("read", true)
                        showCancellationPopup = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CB7C2)
                    )
                ) {
                    Text("I Understand", color = Color.White)
                }
            }
        )
    }

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
            
            // Search bar moved to top for better accessibility
            PainSearchBar(navController = navController)

            CategoriesRow(
                items = listOf(
                    Category("Doctors", R.drawable.ic_doctors),
                    Category("Favorite", R.drawable.ic_favourites),
                    Category("Specialties", R.drawable.ic_specialties),
                    Category("Recent", R.drawable.ic_recent)
                ),
                onCategoryClick = { category ->
                    when (category.label) {
                        "Favorite" -> navController.navigate("favorites_screen")
                        "Doctors" -> navController.navigate("doctor_list/All")
                        "Specialties" -> navController.navigate("patient_specialties")
                        "Recent" -> navController.navigate("recent_doctors")
                    }
                }
            )

            UpcomingSchedule(gradient = gradient, navController = navController)
        }
    }
}

/* ----------------------------- Header ------------------------------ */

@Composable
private fun HeaderCard(
    gradient: Brush,
    firstName: String,
    lastName: String,
    navController: NavController
) {
    val accent = Color(0xFF4CB7C2)

    // Current time & date state
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM") }

    // Update every minute so the clock stays fresh
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(60_000)
        }
    }

    val currentTimeText = currentDateTime.format(timeFormatter)
    val currentDateText = currentDateTime.format(dateFormatter)

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
                    IconBubble(R.drawable.ic_settings) {
                        navController.navigate("settings")
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Center: current time + date
                Column(
                    modifier = Modifier
                        .weight(2f)                 // was 1f → takes more horizontal space
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTimeText,
                        style = MaterialTheme.typography.headlineSmall.copy( // bigger style
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent
                    )
                    Text(
                        text = currentDateText,
                        style = MaterialTheme.typography.bodyMedium.copy(    // a bit bigger than label
                            fontWeight = FontWeight.Medium
                        ),
                        color = accent.copy(alpha = 0.85f)
                    )
                }

                Spacer(Modifier.width(8.dp)) // you can also reduce this from 12.dp

                // Right: greeting + name
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Hi,",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = firstName,
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
            "Search Doctors",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF4CB7C2),
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
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
private fun RowScope.CategoryChip(
    cat: Category,
    onClick: (Category) -> Unit = {}
) {
    val accent = Color(0xFF4CB7C2)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accent)
            ) {
                onClick(cat)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cat.label == "Recent") {
            // Recent uses vector icon, show icon + text separately
            Box(
                modifier = Modifier
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = cat.iconRes),
                    contentDescription = cat.label,
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = "Recent",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            )
        } else {
            // Other categories have text baked into PNG
            Image(
                painter = painterResource(id = cat.iconRes),
                contentDescription = cat.label,
                modifier = Modifier
                    .size(if (cat.label == "Doctors") 48.dp else if (cat.label == "Favorite") 60.dp else 66.dp)
                    .padding(top = 4.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun PainSearchBar(
    navController: NavController
) {
    val accent = Color(0xFF4CB7C2)
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    keyboardController?.hide()
                })
            }
    ) {
        Text(
            text = "Search by Condition",
            style = MaterialTheme.typography.titleMedium.copy(
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g. fever, chest pain, toothache") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        val speciality = mapPainToSpeciality(query)
                        val target = speciality ?: "General Medicine"
                        if (query.isNotBlank()) {
                            navController.navigate("doctor_list/$target")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accent
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = accent.copy(alpha = 0.6f),
                cursorColor = accent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedLabelColor = accent
            )
        )
    }
}

private fun mapPainToSpeciality(raw: String): String? {
    if (raw.isBlank()) return null
    val q = raw.lowercase(Locale.getDefault())

    return when {
        // Cardiology
        listOf(
            "chest pain",
            "chest tightness",
            "chest pressure",
            "heart pain",
            "palpitation",
            "palpitations",
            "rapid heartbeat",
            "fast heartbeat",
            "slow heartbeat",
            "shortness of breath",
            "breathless",
            "breathlessness",
            "bp",
            "blood pressure",
            "high bp",
            "low bp",
            "hypertension",
            "angina",
            "heart"
        ).any { it in q } -> "Cardiology"

        // Dermatology
        listOf(
            "skin",
            "rash",
            "rashes",
            "itch",
            "itching",
            "eczema",
            "psoriasis",
            "pimple",
            "pimples",
            "acne",
            "allergy",
            "allergic",
            "hives",
            "red patches",
            "fungal",
            "ringworm",
            "hair fall",
            "hairfall",
            "dandruff",
            "nail infection"
        ).any { it in q } -> "Dermatology"

        // Gynecology
        listOf(
            "pregnan",
            "pregnancy",
            "pregnant",
            "period",
            "periods",
            "menstrual",
            "menstruation",
            "painful periods",
            "pcos",
            "pcod",
            "fertility",
            "infertility",
            "uterus",
            "uterine",
            "ovary",
            "ovarian",
            "pelvic pain",
            "vaginal",
            "vaginal discharge"
        ).any { it in q } -> "Gynecology"

        // Odontology (dentist)
        listOf(
            "tooth",
            "teeth",
            "toothache",
            "teeth pain",
            "dental",
            "dentist",
            "cavity",
            "cavities",
            "decay",
            "gum",
            "gums",
            "bleeding gum",
            "bleeding gums",
            "jaw",
            "jaw pain",
            "mouth ulcer",
            "wisdom tooth",
            "braces"
        ).any { it in q } -> "Odontology"

        // Oncology
        listOf(
            "cancer",
            "tumor",
            "tumour",
            "lump",
            "mass",
            "chemo",
            "chemotherapy",
            "radiotherapy",
            "onco",
            "leukemia",
            "lymphoma",
            "breast lump",
            "breast cancer"
        ).any { it in q } -> "Oncology"

        // General medicine — fevers, cough, etc.
        listOf(
            "fever",
            "flu",
            "cold",
            "cough",
            "headache",
            "migraine",
            "stomach",
            "stomachache",
            "stomach ache",
            "gas",
            "gastric",
            "indigestion",
            "vomit",
            "vomiting",
            "nausea",
            "diarrhea",
            "diarrhoea",
            "loose motion",
            "body pain",
            "body ache",
            "bodyache",
            "sore throat",
            "throat pain",
            "infection",
            "viral",
            "weakness",
            "tired",
            "fatigue",
            "back pain",
            "leg pain",
            "arm pain",
            "muscle pain",
            "joint pain",
            "burning urine",
            "urine infection",
            "uti",
            "urinary infection",
            "pain"
        ).any { it in q } -> "General Medicine"

        else -> null
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
    val locale = Locale.getDefault()

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load patient’s appointments from Firestore
    LaunchedEffect(Unit) {
        try {
            val profile = AuthManager.getCurrentUserProfile()
            val patientId = profile?.humanId

            appointments = if (!patientId.isNullOrBlank()) {
                UserRepository.getAppointmentsForPatient(patientId)
                    .filter { it.status.equals("booked", ignoreCase = true) }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            appointments = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(Modifier.fillMaxWidth()) {
        // build date chips around today
        val (dates, todayIndex) = remember { generateDateChipsAroundToday(pastDays = 7, futureDays = 7) }
        var displayedMonth by remember { mutableStateOf(monthLabel(dates[todayIndex].date, locale)) }
        var selected by rememberSaveable { mutableIntStateOf(todayIndex) }
        val listState = rememberLazyListState()

        LaunchedEffect(dates) {
            listState.scrollToItem((todayIndex - 2).coerceAtLeast(0))
        }

        LaunchedEffect(listState, dates) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { idx ->
                    val probeIndex = (idx + 2).coerceIn(0, dates.lastIndex)
                    displayedMonth = monthLabel(dates[probeIndex].date, locale)
                }
        }

        // --- Header ---
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

        // --- Date selector ---
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
                    item = dates[i],
                    selected = (i == selected),
                    isToday = dates[i].date == today,
                    onClick = {
                        selected = i
                        displayedMonth = monthLabel(dates[i].date, locale)
                    }
                )
            }
        }

        val selectedDate = dates[selected].date

        // While loading, show the “no appointments” card as a skeleton,
        // or you can drop in a small CircularProgressIndicator if you prefer.
        if (isLoading) {
            NoAppointmentsCard(gradient, selectedDate)
            return@Column
        }

        // Filter Firestore appointments by selected date
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", locale)
        val filtered = appointments.filter { appointment ->
            try {
                val apptDate = LocalDate.parse(appointment.date, dateFormatter)
                apptDate == selectedDate
            } catch (e: Exception) {
                false
            }
        }.sortedBy { parseAppointmentTimeMinutes(it.timing) }  // Sort by time ascending

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
private fun DayPill(item: DayChip, selected: Boolean, isToday: Boolean = false, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF4CCAD1) else Color.Transparent
    val borderColor = when {
        selected -> Color(0xFF4CCAD1)
        isToday -> Color(0xFF2E9E6E) // Green border for today
        else -> Color(0xFF4CB7C2)
    }
    val borderWidth = if (isToday && !selected) 2.dp else 1.dp
    val fg = if (selected) Color.White else Color(0xFF4CB7C2)
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(28.dp))
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
    val appointmentLabel: String,
    val isUpcoming: Boolean       // true if next appointment is in future, false if last appointment
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
    val currentMinutes = remember { LocalTime.now().let { it.hour * 60 + it.minute } }

    // Build list of doctors for this patient from Firestore appointments
    val doctorEntries = remember(appointments, locale, patientId) {
        // 1) Group all appointments by doctor first
        val grouped = appointments
            .filter { appt ->
                val matchesPatient = patientId?.let { appt.patientId == it } ?: true
                val isBooked = appt.status.equals("booked", ignoreCase = true)
                matchesPatient && isBooked
            }
            .groupBy { appt ->
                if (appt.doctorId.isNotBlank()) {
                    appt.doctorId
                } else {
                    listOf(appt.doctorFirstName, appt.doctorLastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "unknown-doctor" }
                }
            }

        // 2) Filter to only include doctors whose most recent appointment is within 10 days
        val filteredGrouped = grouped.filter { (_, list) ->
            val mostRecentDate = list.mapNotNull { appt ->
                parseAppointmentDate(appt.date, locale)
            }.maxOrNull()
            
            mostRecentDate != null && !mostRecentDate.isBefore(today.minusDays(10))
        }

        // 3) map to DoctorRecordEntry
        filteredGrouped.mapNotNull { (key, list) ->
            if (list.isEmpty()) return@mapNotNull null

            // Separate upcoming and past appointments (time-sensitive for today)
            val upcomingAppts = list.filter { appt ->
                val apptDate = parseAppointmentDate(appt.date, locale)
                !isAppointmentPast(apptDate, appt.timing, today, currentMinutes)
            }
            val pastAppts = list.filter { appt ->
                val apptDate = parseAppointmentDate(appt.date, locale)
                isAppointmentPast(apptDate, appt.timing, today, currentMinutes)
            }

            // Determine which appointment to show: next upcoming, or most recent past
            val (displayAppt, isUpcoming) = if (upcomingAppts.isNotEmpty()) {
                // Show the earliest upcoming appointment (by date then time)
                val earliest = upcomingAppts.minWithOrNull(
                    compareBy(
                        { parseAppointmentDate(it.date, locale) ?: LocalDate.MAX },
                        { parseAppointmentTimeMinutes(it.timing) }
                    )
                )!!
                earliest to true
            } else if (pastAppts.isNotEmpty()) {
                // Show the most recent past appointment (by date then time)
                val mostRecent = pastAppts.maxWithOrNull(
                    compareBy(
                        { parseAppointmentDate(it.date, locale) ?: LocalDate.MIN },
                        { parseAppointmentTimeMinutes(it.timing) }
                    )
                )!!
                mostRecent to false
            } else {
                // Fallback to first in list
                list.first() to false
            }

            // If we genuinely have no doctor info, skip
            if (
                displayAppt.doctorId.isBlank() &&
                displayAppt.doctorFirstName.isBlank() &&
                displayAppt.doctorLastName.isBlank()
            ) return@mapNotNull null

            val doctorName = listOf(
                displayAppt.doctorFirstName,
                displayAppt.doctorLastName
            ).filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Unknown doctor" }

            val displayDoctorName = "Dr. $doctorName".trim()

            val displaySpeciality =
                if (displayAppt.doctorSpeciality.isNotBlank())
                    displayAppt.doctorSpeciality
                else
                    "Speciality not set"

            val appointmentLabel = buildString {
                if (displayAppt.date.isNotBlank()) append(displayAppt.date)
                if (displayAppt.timing.isNotBlank()) {
                    if (isNotEmpty()) append("  ")
                    append(displayAppt.timing)
                }
            }.ifBlank { "Not scheduled" }

            DoctorRecordEntry(
                key = key,
                doctorName = displayDoctorName,
                speciality = displaySpeciality,
                appointmentLabel = appointmentLabel,
                isUpcoming = isUpcoming
            )
        }.sortedBy { entry ->
            val datePart = entry.appointmentLabel.substringBefore("  ")
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
                                val encodedName = java.net.URLEncoder.encode(entry.doctorName, "UTF-8")
                                navController.navigate("patient_record_options/${entry.key}/$encodedName")
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
                text = if (entry.isUpcoming) "Next appointment: ${entry.appointmentLabel}" else "Last appointment: ${entry.appointmentLabel}",
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

/**
 * Helper to parse the start time in minutes from strings like "6:00 pm – 9:00 pm" or "4:45 PM"
 * Returns Int.MAX_VALUE if parsing fails.
 */
private fun parseAppointmentTimeMinutes(timing: String): Int {
    val firstPart = timing.split("–", "-", "to").firstOrNull()?.trim() ?: return Int.MAX_VALUE
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

/**
 * Checks if an appointment is in the past based on date and time.
 * For today's appointments, compares the time. For other days, only compares the date.
 */
private fun isAppointmentPast(apptDate: LocalDate?, timing: String, today: LocalDate, currentMinutes: Int): Boolean {
    if (apptDate == null) return false
    return when {
        apptDate.isBefore(today) -> true
        apptDate.isEqual(today) -> {
            val apptMinutes = parseAppointmentTimeMinutes(timing)
            apptMinutes < currentMinutes
        }
        else -> false
    }
}

/* --------------------------- Bottom Bar ---------------------------- */

@Composable
fun PatientBottomBar(
    navController: NavController,
    selectedTab: Int = 0  // 0 = Home, 1 = Chat, 2 = Records, 3 = Schedule
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
                    selected = selectedTab == 0
                ) {
                    navController.navigate("patient_home/$firstName/$lastName") {
                        popUpTo("patient_home/$firstName/$lastName") { inclusive = true }
                    }
                }

                BottomItem(
                    iconRes = R.drawable.ic_messages,
                    label = "Chat",
                    selected = selectedTab == 1
                ) {
                    navController.navigate("chat_selection_patient")
                }

                BottomItem(
                    iconRes = R.drawable.ic_record,
                    label = "Records",
                    selected = selectedTab == 2
                ) {
                    navController.navigate("record_doctor_list")
                }

                BottomItem(
                    iconRes = R.drawable.ic_booking,
                    label = "Schedule",
                    selected = selectedTab == 3
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